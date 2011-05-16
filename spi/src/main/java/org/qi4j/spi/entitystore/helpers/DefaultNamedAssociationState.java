/*
 * Copyright (c) 2009, Rickard Öberg. All Rights Reserved.
 * Copyright (c) 2011, Niclas Hedhman. All Rights Reserved.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.spi.entity.NamedAssociationState;
import org.qi4j.spi.entity.association.NamedEntityReference;

/**
 * Default implementation of NamedAssociationState. Backed by ArrayList.
 */
public final class DefaultNamedAssociationState
    implements NamedAssociationState, Serializable
{
    private final DefaultEntityState entityState;
    private final Map<String, EntityReference> references;

    public DefaultNamedAssociationState( DefaultEntityState entityState, Map<String, EntityReference> references )
    {
        this.entityState = entityState;
        this.references = references;
    }

    public int count()
    {
        return references.size();
    }

    public String contains( EntityReference entityReference )
    {
        for( Map.Entry<String, EntityReference> entry : references.entrySet() )
        {
            if( entityReference.equals( entry.getValue() ) )
            {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean containsKey( String name )
    {
        return references.containsKey( name );
    }

    public void put( String name, EntityReference entityReference )
    {
        references.put( name, entityReference );
        entityState.markUpdated();
    }

    public boolean remove( String name )
    {
        entityState.markUpdated();
        return references.remove( name ) != null;
    }

    public EntityReference get( String name )
    {
        return references.get( name );
    }

    @Override
    public Iterable<String> names()
    {
        return Collections.unmodifiableMap( references ).keySet();
    }

    public Iterator<NamedEntityReference> iterator()
    {
        return new EntityReferenceIterator( references.entrySet().iterator() );
    }

    private class EntityReferenceIterator
        implements Iterator<NamedEntityReference>
    {
        private final Iterator<Map.Entry<String,EntityReference>> iter;

        public EntityReferenceIterator( Iterator<Map.Entry<String,EntityReference>> iter )
        {
            this.iter = iter;
        }

        public boolean hasNext()
        {
            return iter.hasNext();
        }

        public NamedEntityReference next()
        {
            Map.Entry<String, EntityReference> entry = iter.next();
            return new NamedEntityReference( entry.getKey(), entry.getValue() );
        }

        public void remove()
        {
            iter.remove();
            entityState.markUpdated();
        }
    }
}
