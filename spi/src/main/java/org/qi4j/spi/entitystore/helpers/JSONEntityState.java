/*  Copyright 2007-2011 Niclas Hedhman.
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

package org.qi4j.spi.entitystore.helpers;

import java.io.Serializable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.common.TypeName;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.EntityStatus;
import org.qi4j.spi.entity.ManyAssociationState;
import org.qi4j.spi.entity.NamedAssociationState;
import org.qi4j.spi.entitystore.DefaultEntityStoreUnitOfWork;
import org.qi4j.spi.entitystore.EntityStoreException;
import org.qi4j.spi.property.PropertyDescriptor;
import org.qi4j.spi.property.PropertyTypeDescriptor;
import org.qi4j.spi.structure.ModuleSPI;

/**
 * Standard implementation of EntityState.
 */
public final class JSONEntityState
    implements EntityState, Serializable
{
    public static final String JSON_KEY_PROPERTIES = MapEntityStore.JSONKeys.properties.name();
    public static final String JSON_KEY_ASSOCIATIONS = MapEntityStore.JSONKeys.associations.name();
    public static final String JSON_KEY_MANYASSOCIATIONS = MapEntityStore.JSONKeys.manyassociations.name();
    public static final String JSON_KEY_NAMEDASSOCIATIONS = MapEntityStore.JSONKeys.namedassociations.name();
    public static final String JSON_KEY_IDENTITY = MapEntityStore.JSONKeys.identity.name();
    public static final String JSON_KEY_APPLICATION_VERSION = MapEntityStore.JSONKeys.application_version.name();
    public static final String JSON_KEY_TYPE = MapEntityStore.JSONKeys.type.name();
    public static final String JSON_KEY_VERSION = MapEntityStore.JSONKeys.version.name();
    public static final String JSON_KEY_MODIFIED = MapEntityStore.JSONKeys.modified.name();
    private static final String[] EMPTY_NAMES = new String[ 0 ];
    private static final String[] CLONE_NAMES = {
        JSON_KEY_IDENTITY,
        JSON_KEY_APPLICATION_VERSION,
        JSON_KEY_TYPE,
        JSON_KEY_VERSION,
        JSON_KEY_MODIFIED
    };

    protected DefaultEntityStoreUnitOfWork unitOfWork;
    protected EntityStatus status;
    protected String version;

    protected long lastModified;
    private final EntityReference identity;
    private final EntityDescriptor entityDescriptor;
    protected JSONObject state;

    public JSONEntityState( DefaultEntityStoreUnitOfWork unitOfWork,
                            EntityReference identity,
                            EntityDescriptor entityDescriptor,
                            JSONObject initialState
    )
    {
        this( unitOfWork, "",
              unitOfWork.currentTime(),
              identity,
              EntityStatus.NEW,
              entityDescriptor,
              initialState );
    }

    public JSONEntityState( DefaultEntityStoreUnitOfWork unitOfWork,
                            String version,
                            long lastModified,
                            EntityReference identity,
                            EntityStatus status,
                            EntityDescriptor entityDescriptor,
                            JSONObject state
    )
    {
        this.unitOfWork = unitOfWork;
        this.version = version;
        this.lastModified = lastModified;
        this.identity = identity;
        this.status = status;
        this.entityDescriptor = entityDescriptor;
        this.state = state;
    }

    // EntityState implementation

    public final String version()
    {
        return version;
    }

    public long lastModified()
    {
        return lastModified;
    }

    public EntityReference identity()
    {
        return identity;
    }

    public Object getProperty( QualifiedName stateName )
    {
        try
        {
            Object json = state.getJSONObject( JSON_KEY_PROPERTIES ).opt( stateName.name() );
            if( json == null || json == JSONObject.NULL )
            {
                return null;
            }
            else
            {
                ModuleSPI module = unitOfWork.module();
                PropertyDescriptor descriptor = entityDescriptor.state().getPropertyByQualifiedName( stateName );
                return ( (PropertyTypeDescriptor) descriptor ).propertyType().type().fromJSON( json, module );
            }
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public void setProperty( QualifiedName stateName, Object newValue )
    {
        try
        {
            Object jsonValue;
            if( newValue == null )
            {
                jsonValue = JSONObject.NULL;
            }
            else
            {
                PropertyTypeDescriptor propertyDescriptor = entityDescriptor.state()
                    .getPropertyByQualifiedName( stateName );
                jsonValue = propertyDescriptor.propertyType().type().toJSON( newValue );
            }
            cloneStateIfGlobalStateLoaded();
            state.getJSONObject( JSON_KEY_PROPERTIES ).put( stateName.name(), jsonValue );
            markUpdated();
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    private JSONObject cloneJSON( JSONObject jsonObject )
        throws JSONException
    {
        String[] names = JSONObject.getNames( jsonObject );
        if( names == null )
        {
            names = EMPTY_NAMES;
        }
        return new JSONObject( jsonObject, names );
    }

    public EntityReference getAssociation( QualifiedName stateName )
    {
        try
        {
            Object jsonValue = state.getJSONObject( JSON_KEY_ASSOCIATIONS ).opt( stateName.name() );
            if( jsonValue == null || jsonValue == JSONObject.NULL )
            {
                return null;
            }
            else
            {
                return EntityReference.parseEntityReference( (String) jsonValue );
            }
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public void setAssociation( QualifiedName stateName, EntityReference newEntity )
    {
        try
        {
            cloneStateIfGlobalStateLoaded();
            JSONObject jsonObject = state.getJSONObject( JSON_KEY_ASSOCIATIONS );
            jsonObject.put( stateName.name(), newEntity == null ? null : newEntity.identity() );
            markUpdated();
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public ManyAssociationState getManyAssociation( QualifiedName stateName )
    {
        try
        {
            JSONObject manyAssociations = state.getJSONObject( JSON_KEY_MANYASSOCIATIONS );
            JSONArray jsonValues = manyAssociations.optJSONArray( stateName.name() );
            if( jsonValues == null )
            {
                jsonValues = new JSONArray();
                manyAssociations.put( stateName.name(), jsonValues );
            }
            return new JSONManyAssociationState( this, jsonValues );
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    @Override
    public NamedAssociationState getNamedAssociation( QualifiedName stateName )
    {
        try
        {
            JSONObject namedAssociations = state.getJSONObject( JSON_KEY_NAMEDASSOCIATIONS );
            JSONObject jsonValues = namedAssociations.optJSONObject( stateName.name() );
            if( jsonValues == null )
            {
                jsonValues = new JSONObject();
                namedAssociations.put( stateName.name(), jsonValues );
            }
            return new JSONNamedAssociationState( this, jsonValues );
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public void remove()
    {
        status = EntityStatus.REMOVED;
    }

    public EntityStatus status()
    {
        return status;
    }

    public boolean isOfType( TypeName type )
    {
        return entityDescriptor.entityType().type().equals( type );
    }

    public EntityDescriptor entityDescriptor()
    {
        return entityDescriptor;
    }

    public JSONObject state()
    {
        return state;
    }

    @Override
    public String toString()
    {
        return identity + "(" + state + ")";
    }

    public void markUpdated()
    {
        if( status == EntityStatus.LOADED )
        {
            status = EntityStatus.UPDATED;
        }
    }

    boolean isStateNotCloned()
    {
        return status == EntityStatus.LOADED;
    }

    void cloneStateIfGlobalStateLoaded()
    {
        if( isStateNotCloned() )
        {
            return;
        }

        try
        {
            JSONObject newProperties = cloneJSON( state.getJSONObject( JSON_KEY_PROPERTIES ) );
            JSONObject newAssoc = cloneJSON( state.getJSONObject( JSON_KEY_ASSOCIATIONS ) );
            JSONObject newManyAssoc = cloneJSON( state.getJSONObject( JSON_KEY_MANYASSOCIATIONS ) );
            JSONObject newNamedAssoc = cloneJSON( state.getJSONObject( JSON_KEY_NAMEDASSOCIATIONS ) );
            JSONObject stateClone = new JSONObject( state, CLONE_NAMES );
            stateClone.put( JSON_KEY_PROPERTIES, newProperties );
            stateClone.put( JSON_KEY_ASSOCIATIONS, newAssoc );
            stateClone.put( JSON_KEY_MANYASSOCIATIONS, newManyAssoc );
            stateClone.put( JSON_KEY_NAMEDASSOCIATIONS, newNamedAssoc );
            state = stateClone;
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }
}