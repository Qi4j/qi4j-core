/*
 * Copyright (c) 2009, Rickard Öberg. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.qi4j.spi.entitystore.helpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.json.JSONException;
import org.json.JSONObject;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.spi.entity.NamedAssociationState;
import org.qi4j.spi.entity.association.NamedEntityReference;
import org.qi4j.spi.entitystore.EntityStoreException;

/**
 * JSON implementation of ManyAssociationState. Backed by JSONArray.
 */
public final class JSONNamedAssociationState
    implements NamedAssociationState, Serializable
{
    private JSONEntityState entityState;
    private JSONObject references;

    public JSONNamedAssociationState( JSONEntityState entityState, JSONObject references )
    {
        this.entityState = entityState;
        this.references = references;
    }

    public int count()
    {
        return references.length();
    }

    public String contains( EntityReference entityReference )
    {
        try
        {
            String lookingFor = entityReference.identity();
            for( String identity : references )
            {
                if( lookingFor.equals( references.get( identity ) ) )
                {
                    return identity;
                }
            }
            return null;
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    @Override
    public boolean containsKey( String name )
    {
        return false;
    }

    public void put( String name, EntityReference entityReference )
    {
        try
        {
            entityState.cloneStateIfGlobalStateLoaded();
            references.put( name, entityReference.identity() );
            entityState.markUpdated();
        }
        catch( JSONException e )
        {
            throw new EntityStoreException( e );
        }
    }

    public boolean remove( String name )
    {
        entityState.cloneStateIfGlobalStateLoaded();
        references.remove( name );
        entityState.markUpdated();
        return true;
    }

    public EntityReference get( String name )
    {
        String identity = references.optString( name ).trim();
        if( identity == null || identity.length() == 0 )
        {
            return null;
        }
        return new EntityReference( identity );
    }

    @Override
    public Iterable<String> names()
    {
        ArrayList<String> result = new ArrayList<String>();
        for( String value : references )
        {
            result.add( value );
        }
        return result;
    }

    public Iterator<NamedEntityReference> iterator()
    {
        return new EntityReferenceIterator( references.iterator() );
    }

    private static class ReadonlyFacadeIterator
        implements Iterator<String>
    {

        private Iterator<String> keys;

        public ReadonlyFacadeIterator( Iterator<String> keys )
        {
            this.keys = keys;
        }

        @Override
        public boolean hasNext()
        {
            return keys.hasNext();
        }

        @Override
        public String next()
        {
            return keys.next();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    private class EntityReferenceIterator
        implements Iterator<NamedEntityReference>
    {
        private Iterator<String> iterator;

        public EntityReferenceIterator( Iterator<String> iterator )
        {
            this.iterator = iterator;
        }

        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        public NamedEntityReference next()
        {
            try
            {
                String name = iterator.next();
                String identity = references.getString( name );
                EntityReference entityReference = new EntityReference( identity );
                return new NamedEntityReference( name, entityReference );
            }
            catch( JSONException e )
            {
                throw new NoSuchElementException();
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException( "remove() is not supported on NamedAssociation iterators." );
        }
    }
}