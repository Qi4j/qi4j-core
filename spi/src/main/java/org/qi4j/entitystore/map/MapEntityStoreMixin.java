/*
 * Copyright (c) 2009, Rickard Öberg. All Rights Reserved.
 * Copyright (c) 2011, Niclas Hedhman. All Rights Reserved.
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.qi4j.api.cache.CacheOptions;
import org.qi4j.api.common.Optional;
import org.qi4j.api.common.QualifiedName;
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
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.EntityStatus;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.entity.association.AssociationDescriptor;
import org.qi4j.spi.entitystore.DefaultEntityStoreUnitOfWork;
import org.qi4j.spi.entitystore.EntityStore;
import org.qi4j.spi.entitystore.EntityStoreException;
import org.qi4j.spi.entitystore.EntityStoreSPI;
import org.qi4j.spi.entitystore.EntityStoreUnitOfWork;
import org.qi4j.spi.entitystore.StateCommitter;
import org.qi4j.spi.entitystore.helpers.DefaultEntityState;
import org.qi4j.spi.property.PropertyDescriptor;
import org.qi4j.spi.property.PropertyType;
import org.qi4j.spi.property.PropertyTypeDescriptor;
import org.qi4j.spi.service.ServiceDescriptor;
import org.qi4j.spi.structure.ModuleSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of EntityStore that works with an implementation of MapEntityStore. Implement
 * MapEntityStore and add as mixin to the service using this mixin.
 * See {@link org.qi4j.entitystore.memory.MemoryMapEntityStoreMixin} for reference.
 */
public class MapEntityStoreMixin
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

    protected String uuid;
    private int count;

    private Logger logger;

    public void activate()
        throws Exception
    {
        uuid = UUID.randomUUID().toString() + "-";

        logger = LoggerFactory.getLogger( descriptor.identity() );
    }

    public void passivate()
        throws Exception
    {
    }

    // EntityStore

    public EntityStoreUnitOfWork newUnitOfWork( Usecase usecaseMetaInfo, ModuleSPI module, long currentTime )
    {
        return new DefaultEntityStoreUnitOfWork( entityStoreSpi, newUnitOfWorkId(), module, usecaseMetaInfo, currentTime );
    }

    // EntityStoreSPI

    public EntityState newEntityState( EntityStoreUnitOfWork unitOfWork,
                                       EntityReference identity,
                                       EntityDescriptor entityDescriptor
    )
    {
        return new DefaultEntityState( (DefaultEntityStoreUnitOfWork) unitOfWork, identity, entityDescriptor );
    }

    public synchronized EntityState getEntityState( EntityStoreUnitOfWork unitofwork, EntityReference identity )
    {
        DefaultEntityStoreUnitOfWork unitOfWork = (DefaultEntityStoreUnitOfWork) unitofwork;
        Reader in = mapEntityStore.get( identity );
        return readEntityState( unitOfWork, in );
    }

    public StateCommitter applyChanges( final EntityStoreUnitOfWork unitofwork, final Iterable<EntityState> state
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
                            for( EntityState entityState : state )
                            {
                                DefaultEntityState state = (DefaultEntityState) entityState;
                                if( state.status().equals( EntityStatus.NEW ) )
                                {
                                    Writer writer = changer.newEntity( state.identity(),
                                                                       state.entityDescriptor().entityType() );
                                    writeEntityState( state, writer, unitofwork.identity(), unitofwork.currentTime() );
                                    writer.close();
                                }
                                else if( state.status().equals( EntityStatus.UPDATED ) )
                                {
                                    Writer writer = changer.updateEntity( state.identity(),
                                                                          state.entityDescriptor().entityType() );
                                    writeEntityState( state, writer, unitofwork.identity(), unitofwork.currentTime() );
                                    writer.close();
                                }
                                else if( state.status().equals( EntityStatus.REMOVED ) )
                                {
                                    changer.removeEntity( state.identity(), state.entityDescriptor().entityType() );
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
            public <ReceiverThrowableType extends Throwable> void transferTo( Output<? super EntityState, ReceiverThrowableType> output )
                throws EntityStoreException, ReceiverThrowableType
            {
                output.receiveFrom( new Sender<EntityState, EntityStoreException>()
                {
                    @Override
                    public <ReceiverThrowableType extends Throwable> void sendTo( final Receiver<? super EntityState, ReceiverThrowableType> receiver )
                        throws ReceiverThrowableType, EntityStoreException
                    {
                        Usecase usecase = UsecaseBuilder
                            .buildUsecase( "qi4j.entitystore.entitystates" )
                            .with( CacheOptions.NEVER )
                            .newUsecase();

                        final DefaultEntityStoreUnitOfWork uow =
                            new DefaultEntityStoreUnitOfWork( entityStoreSpi, newUnitOfWorkId(), module, usecase, System
                                .currentTimeMillis() );

                        final List<EntityState> migrated = new ArrayList<EntityState>();

                        try
                        {
                            mapEntityStore.entityStates().transferTo( new Output<Reader, ReceiverThrowableType>()
                            {
                                @Override
                                public <SenderThrowableType extends Throwable> void receiveFrom( Sender<? extends Reader, SenderThrowableType> sender )
                                    throws ReceiverThrowableType, SenderThrowableType
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
                        DefaultEntityState state = (DefaultEntityState) migratedEntity;
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

    protected void writeEntityState( DefaultEntityState state, Writer writer, String identity, long lastModified )
        throws EntityStoreException
    {
        try
        {
            JSONWriter json = new JSONWriter( writer );
            JSONWriter properties = json.object().
                key( "identity" ).value( state.identity().identity() ).
                key( "application_version" ).value( application.version() ).
                key( "type" ).value( state.entityDescriptor().entityType().type().name() ).
                key( "version" ).value( identity ).
                key( "modified" ).value( lastModified ).
                key( "properties" ).object();
            EntityType entityType = state.entityDescriptor().entityType();
            for( PropertyType propertyType : entityType.properties() )
            {
                Object value = state.properties().get( propertyType.qualifiedName() );
                json.key( propertyType.qualifiedName().name() );
                if( value == null )
                {
                    json.value( null );
                }
                else
                {
                    propertyType.type().toJSON( value, json );
                }
            }

            JSONWriter associations = properties.endObject().key( "associations" ).object();
            for( Map.Entry<QualifiedName, EntityReference> stateNameEntityReferenceEntry : state.associations()
                .entrySet() )
            {
                EntityReference value = stateNameEntityReferenceEntry.getValue();
                associations.key( stateNameEntityReferenceEntry.getKey().name() ).
                    value( value != null ? value.identity() : null );
            }

            JSONWriter manyAssociations = associations.endObject().key( "manyassociations" ).object();
            for( Map.Entry<QualifiedName, List<EntityReference>> stateNameListEntry : state.manyAssociations()
                .entrySet() )
            {
                JSONWriter assocs = manyAssociations.key( stateNameListEntry.getKey().name() ).array();
                for( EntityReference entityReference : stateNameListEntry.getValue() )
                {
                    assocs.value( entityReference.identity() );
                }
                assocs.endArray();
            }

            JSONWriter namedAssociations = manyAssociations.endObject().key( "namedassociations" ).object();
            for( Map.Entry<QualifiedName, Map<String, EntityReference>> stateNameListEntry : state.namedAssociations()
                .entrySet() )
            {
                JSONWriter assocs = manyAssociations.key( stateNameListEntry.getKey().name() ).array();
                for( EntityReference entityReference : stateNameListEntry.getValue().values() )
                {
                    assocs.value( entityReference.identity() );
                }
                assocs.endArray();
            }
            namedAssociations.endObject().endObject();
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( "Could not store EntityState", e );
        }
    }

    protected EntityState readEntityState( DefaultEntityStoreUnitOfWork unitOfWork, Reader entityState )
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

                LoggerFactory.getLogger( MapEntityStoreMixin.class )
                    .debug(
                        "Updated version nr on " + identity + " from " + currentAppVersion + " to " + application.version() );

                // State changed
                status = EntityStatus.UPDATED;
            }

            String type = jsonObject.getString( "type" );

            EntityDescriptor entityDescriptor = module.entityDescriptor( type );
            if( entityDescriptor == null )
            {
                throw new EntityTypeNotFoundException( type );
            }

            Map<QualifiedName, Object> properties = new HashMap<QualifiedName, Object>();
            JSONObject props = jsonObject.getJSONObject( "properties" );
            for( PropertyDescriptor propertyDescriptor : entityDescriptor.state().properties() )
            {
                Object jsonValue;
                try
                {
                    jsonValue = props.get( propertyDescriptor.qualifiedName().name() );
                }
                catch( JSONException e )
                {
                    // Value not found, default it
                    Object initialValue = propertyDescriptor.initialValue();
                    properties.put( propertyDescriptor.qualifiedName(), initialValue );
                    status = EntityStatus.UPDATED;
                    continue;
                }
                if( jsonValue == JSONObject.NULL )
                {
                    properties.put( propertyDescriptor.qualifiedName(), null );
                }
                else
                {
                    Object value = ( (PropertyTypeDescriptor) propertyDescriptor ).propertyType()
                        .type()
                        .fromJSON( jsonValue, module );
                    properties.put( propertyDescriptor.qualifiedName(), value );
                }
            }

            Map<QualifiedName, EntityReference> associations = new HashMap<QualifiedName, EntityReference>();
            JSONObject assocs = jsonObject.getJSONObject( "associations" );
            for( AssociationDescriptor associationType : entityDescriptor.state().associations() )
            {
                try
                {
                    Object jsonValue = assocs.get( associationType.qualifiedName().name() );
                    EntityReference value = jsonValue == JSONObject.NULL ? null : EntityReference.parseEntityReference(
                        (String) jsonValue );
                    associations.put( associationType.qualifiedName(), value );
                }
                catch( JSONException e )
                {
                    // Association not found, default it to null
                    associations.put( associationType.qualifiedName(), null );
                    status = EntityStatus.UPDATED;
                }
            }

            Map<QualifiedName, List<EntityReference>> manyAssociations = createManyAssociations( jsonObject, entityDescriptor );
            Map<QualifiedName, Map<String, EntityReference>> namedAssociations = createNamedAssociations( jsonObject, entityDescriptor );

            return new DefaultEntityState( unitOfWork,
                                           version,
                                           modified,
                                           EntityReference.parseEntityReference( identity ),
                                           status,
                                           entityDescriptor,
                                           properties,
                                           associations,
                                           manyAssociations,
                                           namedAssociations
            );
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    private Map<QualifiedName, List<EntityReference>> createManyAssociations( JSONObject jsonObject,
                                                                              EntityDescriptor entityDescriptor
    )
        throws JSONException
    {
        Map<QualifiedName, List<EntityReference>> manyAssociations = new HashMap<QualifiedName, List<EntityReference>>();
        JSONObject manyAssocs = jsonObject.getJSONObject( "manyassociations" );
        for( AssociationDescriptor manyAssociationType : entityDescriptor.state().manyAssociations() )
        {
            List<EntityReference> references = new ArrayList<EntityReference>();
            try
            {
                JSONArray jsonValues = manyAssocs.getJSONArray( manyAssociationType.qualifiedName().name() );
                for( int i = 0; i < jsonValues.length(); i++ )
                {
                    Object jsonValue = jsonValues.getString( i );
                    EntityReference value = jsonValue == JSONObject.NULL ? null : EntityReference.parseEntityReference(
                        (String) jsonValue );
                    references.add( value );
                }
                manyAssociations.put( manyAssociationType.qualifiedName(), references );
            }
            catch( JSONException e )
            {
                // ManyAssociation not found, default to empty one
                manyAssociations.put( manyAssociationType.qualifiedName(), references );
            }
        }
        return manyAssociations;
    }

    private Map<QualifiedName, Map<String, EntityReference>> createNamedAssociations( JSONObject jsonObject,
                                                                                      EntityDescriptor entityDescriptor
    )
        throws JSONException
    {
        Map<QualifiedName, Map<String, EntityReference>> namedAssociations = new HashMap<QualifiedName, Map<String, EntityReference>>();
        if( !jsonObject.has( "namedassociations" ) )
        {
            return namedAssociations;
        }
        JSONObject namedAssocs = jsonObject.getJSONObject( "namedassociations" );
        for( AssociationDescriptor namedAssociationType : entityDescriptor.state().namedAssociations() )
        {
            Map<String, EntityReference> references = new HashMap<String, EntityReference>();
            try
            {
                JSONObject jsonValues = namedAssocs.getJSONObject( namedAssociationType.qualifiedName().name() );
                for( String name : jsonValues )
                {
                    Object jsonValue = jsonValues.getString( name );
                    EntityReference value = jsonValue == JSONObject.NULL ? null : EntityReference.parseEntityReference(
                        (String) jsonValue );
                    references.put( name, value );
                }
                namedAssociations.put( namedAssociationType.qualifiedName(), references );
            }
            catch( JSONException e )
            {
                // NamedAssociation not found, default to empty one
                namedAssociations.put( namedAssociationType.qualifiedName(), references );
            }
        }
        return namedAssociations;
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
}
