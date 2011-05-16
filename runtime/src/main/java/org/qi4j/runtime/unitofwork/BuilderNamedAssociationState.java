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

package org.qi4j.runtime.unitofwork;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.spi.entity.NamedAssociationState;
import org.qi4j.spi.entity.association.NamedEntityReference;

/**
 * Default implementation of ManyAssociationState that also
 * keeps a list of changes that can be extracted at any time.
 */
public final class BuilderNamedAssociationState
    implements NamedAssociationState
{
    private Map<String, EntityReference> references;

    public BuilderNamedAssociationState( NamedAssociationState state )
    {
        // Copy
        for( NamedEntityReference ref : state )
        {
            references.put( ref.name(), ref.entityReference() );
        }
    }

    public BuilderNamedAssociationState()
    {
        references = new HashMap<String, EntityReference>();
    }

    public int count()
    {
        return references.size();
    }

    public String contains( EntityReference entityReference )
    {
        if( entityReference != null )
        {
            for( Map.Entry<String, EntityReference> entry : references.entrySet() )
            {
                if( entityReference.equals( entry.getValue() ) )
                {
                    return entry.getKey();
                }
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
    }

    public boolean remove( String name )
    {
        return references.remove( name ) != null;
    }

    @Override
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
        return new NamedEntityReferenceIterator( references.entrySet().iterator() );
    }

    private static class NamedEntityReferenceIterator
        implements Iterator<NamedEntityReference>
    {
        private Iterator<Map.Entry<String, EntityReference>> entries;

        public NamedEntityReferenceIterator( Iterator<Map.Entry<String, EntityReference>> entries )
        {
            this.entries = entries;
        }

        @Override
        public boolean hasNext()
        {
            return entries.hasNext();
        }

        @Override
        public NamedEntityReference next()
        {
            Map.Entry<String, EntityReference> next = entries.next();
            return new NamedEntityReference( next.getKey(), next.getValue() );
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}