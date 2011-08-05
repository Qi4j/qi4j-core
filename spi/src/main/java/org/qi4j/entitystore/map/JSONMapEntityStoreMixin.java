/*
 * Copyright (c) 2009, Rickard Öberg. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entitystore.map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.qi4j.api.cache.CacheOptions;
import org.qi4j.api.common.Optional;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.injection.scope.Uses;
import org.qi4j.api.io.Input;
import org.qi4j.api.io.Output;
import org.qi4j.api.io.Receiver;
import org.qi4j.api.io.Sender;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.structure.Application;
import org.qi4j.api.unitofwork.EntityTypeNotFoundException;
import org.qi4j.api.usecase.Usecase;
import org.qi4j.api.usecase.UsecaseBuilder;
import org.qi4j.spi.cache.Cache;
import org.qi4j.spi.cache.CachePool;
import org.qi4j.spi.cache.NullCache;
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.EntityStatus;
import org.qi4j.spi.entitystore.*;
import org.qi4j.spi.entitystore.helpers.JSONEntityState;
import org.qi4j.spi.service.ServiceDescriptor;
import org.qi4j.spi.structure.ModuleSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of EntityStore that works with an implementation of MapEntityStore. Implement
 * MapEntityStore and add as mixin to the service using this mixin.
 * See {@link org.qi4j.entitystore.memory.MemoryMapEntityStoreMixin} for reference.
 */
public class JSONMapEntityStoreMixin
    implements EntityStore, EntityStoreSPI, StateStore, Activatable
{
    @This
    private MapEntityStore mapEntityStore;

    @This
    private EntityStoreSPI entityStoreSpi;

    @Structure
    private Application application;

    @Optional
    @Service
    private Migration migration;

    @Uses
    private ServiceDescriptor descriptor;

    @Optional
    @Service
    private CachePool caching;
    private Cache<JSONObject> cache;

    protected String uuid;
    private int count;

    private Logger logger;

    public JSONMapEntityStoreMixin()
    {
    }

    public void activate()
        throws Exception
    {
        logger = LoggerFactory.getLogger( descriptor.identity() );

        uuid = UUID.randomUUID().toString() + "-";
        if( caching != null )
        {
            cache = caching.fetchCache( uuid, JSONObject.class );
        }
        else
        {
            cache = new NullCache<JSONObject>();
        }
    }

    public void passivate()
        throws Exception
    {
        if( caching != null )
        {
            caching.returnCache( cache );
        }
    }

    // EntityStore

    public EntityStoreUnitOfWork newUnitOfWork( Usecase usecaseMetaInfo, ModuleSPI module, long currentTime )
    {
        return new DefaultEntityStoreUnitOfWork( entityStoreSpi, newUnitOfWorkId(), module, usecaseMetaInfo, currentTime );
    }

    // EntityStoreSPI

    public EntityState newEntityState( EntityStoreUnitOfWork unitOfWork,
                                       EntityReference identity,
                                       EntityDescriptor entityDescriptor)
    {
        try
        {
            JSONObject state = new JSONObject();
            state.put( JSONEntityState.JSON_KEY_IDENTITY, identity.identity() );
            state.put( JSONEntityState.JSON_KEY_APPLICATION_VERSION, application.version() );
            state.put( JSONEntityState.JSON_KEY_TYPE, entityDescriptor.entityType().type().name() );
            state.put( JSONEntityState.JSON_KEY_VERSION, unitOfWork.identity() );
            state.put( JSONEntityState.JSON_KEY_MODIFIED, unitOfWork.currentTime() );
            state.put( JSONEntityState.JSON_KEY_PROPERTIES, new JSONObject() );
            state.put( JSONEntityState.JSON_KEY_ASSOCIATIONS, new JSONObject() );
            state.put( JSONEntityState.JSON_KEY_MANYASSOCIATIONS, new JSONObject() );
            state.put( JSONEntityState.JSON_KEY_NAMEDASSOCIATIONS, new JSONObject() );
            return new JSONEntityState( (DefaultEntityStoreUnitOfWork) unitOfWork, identity, entityDescriptor, state );
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public synchronized EntityState getEntityState( EntityStoreUnitOfWork unitOfWork, EntityReference identity )
    {
        EntityState state = fetchCachedState( identity, (DefaultEntityStoreUnitOfWork) unitOfWork );
        if( state != null )
        {
            return state;
        }
        // Get state
        Reader in = mapEntityStore.get( identity );
        JSONEntityState loadedState = readEntityState( (DefaultEntityStoreUnitOfWork) unitOfWork, in );
        if( doCacheOnRead( (DefaultEntityStoreUnitOfWork) unitOfWork ) )
        {
            cache.put( identity.identity(), loadedState.state() );
        }
        return loadedState;
    }

    public StateCommitter applyChanges( final EntityStoreUnitOfWork unitOfWork, final Iterable<EntityState> state
    )
        throws EntityStoreException
    {
        return new StateCommitter()
        {
            public void commit()
            {
                try
                {
                    mapEntityStore.applyChanges( new MapEntityStore.MapChanges()
                    {
                        public void visitMap( MapEntityStore.MapChanger changer )
                            throws IOException
                        {
                            DefaultEntityStoreUnitOfWork uow = (DefaultEntityStoreUnitOfWork) unitOfWork;
                            CacheOptions options = uow.usecase().metaInfo( CacheOptions.class );
                            if( options == null )
                            {
                                options = CacheOptions.ALWAYS;
                            }

                            for( EntityState entityState : state )
                            {
                                JSONEntityState state = (JSONEntityState) entityState;
                                if( state.status().equals( EntityStatus.NEW ) )
                                {
                                    Writer writer = changer.newEntity( state.identity(),
                                                                       state.entityDescriptor().entityType() );
                                    writeEntityState( state, writer, unitOfWork.identity(), unitOfWork.currentTime() );
                                    writer.close();
                                    if( options.cacheOnNew() )
                                    {
                                        cache.put( state.identity().identity(), state.state() );
                                    }
                                }
                                else if( state.status().equals( EntityStatus.UPDATED ) )
                                {
                                    Writer writer = changer.updateEntity( state.identity(),
                                                                          state.entityDescriptor().entityType() );
                                    writeEntityState( state, writer, unitOfWork.identity(), unitOfWork.currentTime() );
                                    writer.close();
                                    if( options.cacheOnWrite() )
                                    {
                                        cache.put( state.identity().identity(), state.state() );
                                    }
                                }
                                else if( state.status().equals( EntityStatus.REMOVED ) )
                                {
                                    changer.removeEntity( state.identity(), state.entityDescriptor().entityType() );
                                    cache.remove( state.identity().identity() );
                                }
                            }
                        }
                    } );
                }
                catch( IOException e )
                {
                    throw new EntityStoreException( e );
                }
            }

            public void cancel()
            {
            }
        };
    }

    public Input<EntityState, EntityStoreException> entityStates( final ModuleSPI module )
    {
        return new Input<EntityState, EntityStoreException>()
        {
           @Override
           public <ReceiverThrowableType extends Throwable> void transferTo(Output<? super EntityState, ReceiverThrowableType> output) throws EntityStoreException, ReceiverThrowableType
           {
                output.receiveFrom( new Sender<EntityState, EntityStoreException>()
                {
                   @Override
                   public <ReceiverThrowableType extends Throwable> void sendTo(final Receiver<? super EntityState, ReceiverThrowableType> receiver) throws ReceiverThrowableType, EntityStoreException
                   {
                        Usecase usecase = UsecaseBuilder
                            .buildUsecase( "qi4j.entitystore.entitystates" )
                            .with( CacheOptions.NEVER )
                            .newUsecase();

                        final DefaultEntityStoreUnitOfWork uow =
                            new DefaultEntityStoreUnitOfWork( entityStoreSpi, newUnitOfWorkId(), module, usecase, System.currentTimeMillis() );

                        final List<EntityState> migrated = new ArrayList<EntityState>();

                        try
                        {
                            mapEntityStore.entityStates().transferTo( new Output<Reader, ReceiverThrowableType>()
                            {
                               @Override
                               public <SenderThrowableType extends Throwable> void receiveFrom(Sender<? extends Reader, SenderThrowableType> sender) throws ReceiverThrowableType, SenderThrowableType
                               {
                                    sender.sendTo( new Receiver<Reader, ReceiverThrowableType>()
                                    {
                                        public void receive( Reader item )
                                            throws ReceiverThrowableType
                                        {
                                            final EntityState entity = readEntityState( uow, item );
                                            if( entity.status() == EntityStatus.UPDATED )
                                            {
                                                migrated.add( entity );

                                                // Synch back 100 at a time
                                                if( migrated.size() > 100 )
                                                {
                                                    synchMigratedEntities( migrated );
                                                }
                                            }
                                            receiver.receive( entity );
                                        }
                                    } );

                                    // Synch any remaining migrated entities
                                    if( !migrated.isEmpty() )
                                    {
                                        synchMigratedEntities( migrated );
                                    }
                                }
                            } );
                        }
                        catch( IOException e )
                        {
                            throw new EntityStoreException( e );
                        }
                    }
                } );
            }
        };
    }

    private void synchMigratedEntities( final List<EntityState> migratedEntities )
    {
        try
        {
            mapEntityStore.applyChanges( new MapEntityStore.MapChanges()
            {
                public void visitMap( MapEntityStore.MapChanger changer )
                    throws IOException
                {
                    for( EntityState migratedEntity : migratedEntities )
                    {
                        JSONEntityState state = (JSONEntityState) migratedEntity;
                        Writer writer = changer.updateEntity( state.identity(),
                                                              state.entityDescriptor().entityType() );
                        writeEntityState( state, writer, state.version(), state.lastModified() );
                        writer.close();
                    }
                }
            } );
            migratedEntities.clear();
        }
        catch( IOException e )
        {
            logger.warn( "Could not store migrated entites", e );
        }
    }

    protected String newUnitOfWorkId()
    {
        return uuid + Integer.toHexString( count++ );
    }

    protected void writeEntityState( JSONEntityState state, Writer writer, String identity, long lastModified )
        throws EntityStoreException
    {
        try
        {
            JSONObject jsonState = state.state();
            jsonState.put( "version", identity );
            jsonState.put( "modified", lastModified );
            writer.append( jsonState.toString() );
        }
        catch( Exception e )
        {
            throw new EntityStoreException( "Could not store EntityState", e );
        }
    }

    protected JSONEntityState readEntityState( DefaultEntityStoreUnitOfWork unitOfWork, Reader entityState )
        throws EntityStoreException
    {
        try
        {
            ModuleSPI module = unitOfWork.module();
            JSONObject jsonObject = new JSONObject( new JSONTokener( entityState ) );
            EntityStatus status = EntityStatus.LOADED;

            String version = jsonObject.getString( "version" );
            long modified = jsonObject.getLong( "modified" );
            String identity = jsonObject.getString( "identity" );

            // Check if version is correct
            String currentAppVersion = jsonObject.optString( MapEntityStore.JSONKeys.application_version.name(),
                                                             "0.0" );
            if( !currentAppVersion.equals( application.version() ) )
            {
                if( migration != null )
                {
                    migration.migrate( jsonObject, application.version(), this );
                }
                else
                {
                    // Do nothing - set version to be correct
                    jsonObject.put( MapEntityStore.JSONKeys.application_version.name(), application.version() );
                }

                String msg = "Updated version nr on " + identity + " from " + currentAppVersion + " to " + application.version();
                LoggerFactory.getLogger( getClass() ).debug(msg );

                // State changed
                status = EntityStatus.UPDATED;
            }

            String type = jsonObject.getString( "type" );

            EntityDescriptor entityDescriptor = module.entityDescriptor( type );
            if( entityDescriptor == null )
            {
                throw new EntityTypeNotFoundException( type );
            }

            return new JSONEntityState( unitOfWork,
                                        version,
                                        modified,
                                        EntityReference.parseEntityReference( identity ),
                                        status,
                                        entityDescriptor,
                                        jsonObject
            );
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public JSONObject getState( String id )
        throws IOException
    {
        Reader reader = mapEntityStore.get( EntityReference.parseEntityReference( id ) );
        JSONObject jsonObject;
        try
        {
            jsonObject = new JSONObject( new JSONTokener( reader ) );
        }
        catch( JSONException e )
        {
            throw (IOException) new IOException().initCause( e );
        }
        reader.close();
        return jsonObject;
    }

    private EntityState fetchCachedState( EntityReference identity, DefaultEntityStoreUnitOfWork unitOfWork )
    {
        JSONObject data = cache.get( identity.identity() );
        if( data != null )
        {
            try
            {
                String type = data.getString( "type" );
                EntityDescriptor entityDescriptor = unitOfWork.module().entityDescriptor( type );
                return new JSONEntityState( unitOfWork, identity, entityDescriptor, data );
            }
            catch( JSONException e )
            {
                // Should not be able to happen, unless internal error in the cache system.
                throw new EntityStoreException( e );
            }
        }
        return null;
    }

    private boolean doCacheOnRead( DefaultEntityStoreUnitOfWork unitOfWork )
    {
        CacheOptions cacheOptions = ( unitOfWork ).usecase().metaInfo( CacheOptions.class );
        return cacheOptions == null || cacheOptions.cacheOnRead();
    }
}