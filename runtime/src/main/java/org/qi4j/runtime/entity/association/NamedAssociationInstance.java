/*
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
 */

package org.qi4j.runtime.entity.association;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.qi4j.api.entity.Identity;
import org.qi4j.api.entity.association.AssociationInfo;
import org.qi4j.api.entity.association.NamedAssociation;
import org.qi4j.runtime.structure.ModuleUnitOfWork;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.NamedAssociationState;
import org.qi4j.spi.entity.association.NamedEntityReference;

/**
 * JAVADOC
 */
public final class NamedAssociationInstance<T>
    extends AbstractAssociationInstance<T>
    implements NamedAssociation<T>
{
    private NamedAssociationModel model;

    public NamedAssociationInstance( AssociationInfo associationInfo,
                                     NamedAssociationModel constraints,
                                     ModuleUnitOfWork unitOfWork,
                                     EntityState entityState
    )
    {
        super( associationInfo, unitOfWork, entityState );
        this.model = constraints;
    }

    public int count()
    {
        return associated().count();
    }

    public String contains( T entity )
    {
        return associated().contains( getEntityReference( entity ) );
    }

    @Override
    public Iterable<String> names()
    {
        return associated().names();
    }

    public void put( String name, T entity )
    {
        checkImmutable();
        checkType( entity );
        model.checkConstraints( entity );
        try
        {
            associated().put( name, getEntityReference( entity ) );
        }
        finally
        {
            model.checkAssociationConstraints( this );
        }
    }

    public boolean remove( T entity )
    {
        checkImmutable();
        checkType( entity );

        try
        {
            return associated().remove( ( (Identity) entity ).identity().get() );
        }
        finally
        {
            model.checkAssociationConstraints( this );
        }
    }

    public T get( String name )
    {
        return getEntity( associated().get( name ) );
    }

    public Map<String, T> toMap()
    {
        HashMap<String, T> result = new HashMap<String, T>();
        for( NamedEntityReference reference : associated() )
        {
            result.put( reference.name(), getEntity( reference.entityReference() ) );
        }

        return result;
    }

    public String toString()
    {
        return associated().toString();
    }

    public Iterator<String> iterator()
    {
        return new NamedAssociationIterator( associated().iterator() );
    }

    @Override
    public boolean containsKey( String name )
    {
        return associated().containsKey( name );
    }

    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        if( !super.equals( o ) )
        {
            return false;
        }
        NamedAssociationInstance that = (NamedAssociationInstance) o;
        return model.equals( that.model );
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + model.hashCode();
        return result;
    }

    protected class NamedAssociationIterator
        implements Iterator<String>
    {
        private final Iterator<NamedEntityReference> idIterator;

        public NamedAssociationIterator( Iterator<NamedEntityReference> idIterator )
        {
            this.idIterator = idIterator;
        }

        public boolean hasNext()
        {
            return idIterator.hasNext();
        }

        public String next()
        {
            return idIterator.next().name();
        }

        public void remove()
        {
            checkImmutable();
            idIterator.remove();
        }
    }

    @Override
    protected void checkType( Object instance )
    {
        if( instance == null )
        {
            throw new NullPointerException( "Associated object may not be null" );
        }

        super.checkType( instance );
    }

    protected boolean isSet()
    {
        return true;
    }

    private NamedAssociationState associated()
    {
        return entityState.getNamedAssociation( ( model ).associationType().qualifiedName() );
    }
}
