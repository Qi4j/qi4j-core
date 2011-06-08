/*
 * Copyright (c) 2008, Rickard Öberg. All Rights Reserved.
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

package org.qi4j.runtime.entity;

import java.lang.reflect.Method;
import java.util.Set;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.entity.association.Association;
import org.qi4j.api.entity.association.EntityStateHolder;
import org.qi4j.api.entity.association.ManyAssociation;
import org.qi4j.api.entity.association.NamedAssociation;
import org.qi4j.api.property.Property;
import org.qi4j.runtime.composite.AbstractStateModel;
import org.qi4j.runtime.entity.association.EntityAssociationsInstance;
import org.qi4j.runtime.entity.association.EntityAssociationsModel;
import org.qi4j.runtime.entity.association.EntityManyAssociationsInstance;
import org.qi4j.runtime.entity.association.EntityManyAssociationsModel;
import org.qi4j.runtime.entity.association.EntityNamedAssociationsInstance;
import org.qi4j.runtime.entity.association.EntityNamedAssociationsModel;
import org.qi4j.runtime.structure.ModuleUnitOfWork;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.EntityStateDescriptor;
import org.qi4j.spi.entity.association.AssociationDescriptor;
import org.qi4j.spi.entity.association.AssociationType;
import org.qi4j.spi.entity.association.ManyAssociationDescriptor;
import org.qi4j.spi.entity.association.ManyAssociationType;
import org.qi4j.spi.entity.association.NamedAssociationDescriptor;
import org.qi4j.spi.entity.association.NamedAssociationType;
import org.qi4j.spi.property.PropertyType;

/**
 * JAVADOC
 */
public final class EntityStateModel
    extends AbstractStateModel<EntityPropertiesModel>
    implements EntityStateDescriptor
{
    private final EntityAssociationsModel associationsModel;
    private final EntityManyAssociationsModel manyAssociationsModel;
    private final EntityNamedAssociationsModel namedAssociationsModel;

    public EntityStateModel( EntityPropertiesModel propertiesModel,
                             EntityAssociationsModel associationsModel,
                             EntityManyAssociationsModel manyAssociationsModel,
                             EntityNamedAssociationsModel namedAssociationsModel
    )
    {
        super( propertiesModel );
        this.associationsModel = associationsModel;
        this.manyAssociationsModel = manyAssociationsModel;
        this.namedAssociationsModel = namedAssociationsModel;
    }

    public EntityStateModel.EntityStateInstance newInstance( ModuleUnitOfWork uow, EntityState entityState )
    {
        return new EntityStateInstance( propertiesModel.newInstance( entityState ),
                                        associationsModel.newInstance( entityState, uow ),
                                        manyAssociationsModel.newInstance( entityState, uow ),
                                        namedAssociationsModel.newInstance( entityState, uow ) );
    }

    @Override
    public void addStateFor( Iterable<Method> methods, Class compositeType )
    {
        super.addStateFor( methods, compositeType );
        for( Method method : methods )
        {
            associationsModel.addAssociationFor( method );
        }
        for( Method method : methods )
        {
            manyAssociationsModel.addManyAssociationFor( method );
        }
        for( Method method : methods )
        {
            namedAssociationsModel.addNamedAssociationFor( method );
        }
    }

    public AssociationDescriptor getAssociationByName( String name )
    {
        return associationsModel.getAssociationByName( name );
    }

    public ManyAssociationDescriptor getManyAssociationByName( String name )
    {
        return manyAssociationsModel.getManyAssociationByName( name );
    }

    @Override
    public NamedAssociationDescriptor getNamedAssociationByName( String name )
    {
        return namedAssociationsModel.getNamedAssociationByName( name );
    }

    public <T extends AssociationDescriptor> Set<T> associations()
    {
        return associationsModel.associations();
    }

    public <T extends ManyAssociationDescriptor> Set<T> manyAssociations()
    {
        return manyAssociationsModel.manyAssociations();
    }

    @Override
    public <T extends NamedAssociationDescriptor> Set<T> namedAssociations()
    {
        return namedAssociationsModel.namedAssociations();
    }

    public Set<PropertyType> propertyTypes()
    {
        return propertiesModel.propertyTypes();
    }

    public Set<AssociationType> associationTypes()
    {
        return associationsModel.associationTypes();
    }

    public Set<ManyAssociationType> manyAssociationTypes()
    {
        return manyAssociationsModel.manyAssociationTypes();
    }

    public Set<NamedAssociationType> namedAssociationTypes()
    {
        return namedAssociationsModel.namedAssociationTypes();
    }

    public final class EntityStateInstance
        implements EntityStateHolder
    {
        private final EntityPropertiesInstance entityPropertiesInstance;
        private final EntityAssociationsInstance entityAssociationsInstance;
        private final EntityManyAssociationsInstance entityManyAssociationsInstance;
        private final EntityNamedAssociationsInstance entityNamedAssociationsInstance;

        private EntityStateInstance(
            EntityPropertiesInstance entityPropertiesInstance,
            EntityAssociationsInstance entityAssociationsInstance,
            EntityManyAssociationsInstance entityManyAssociationsInstance,
            EntityNamedAssociationsInstance entityNamedAssociationsInstance
        )
        {
            this.entityPropertiesInstance = entityPropertiesInstance;
            this.entityAssociationsInstance = entityAssociationsInstance;
            this.entityManyAssociationsInstance = entityManyAssociationsInstance;
            this.entityNamedAssociationsInstance = entityNamedAssociationsInstance;
        }

        public <T> Property<T> getProperty( Method accessor )
        {
            return entityPropertiesInstance.<T>getProperty( accessor );
        }

        public <T> Property<T> getProperty( QualifiedName name )
        {
            return entityPropertiesInstance.getProperty( name );
        }

        public <T> Association<T> getAssociation( Method accessor )
        {
            return entityAssociationsInstance.associationFor( accessor );
        }

        public <T> ManyAssociation<T> getManyAssociation( Method accessor )
        {
            return entityManyAssociationsInstance.manyAssociationFor( accessor );
        }

        @Override
        public <T> NamedAssociation<T> getNamedAssociation( Method accessor )
        {
            return entityNamedAssociationsInstance.namedAssociationFor( accessor );
        }

        public <ThrowableType extends Throwable> void visitState( EntityStateVisitor<ThrowableType> visitor )
            throws ThrowableType
        {
            visitProperties( visitor );

            entityAssociationsInstance.visitAssociations( visitor );
            entityManyAssociationsInstance.visitManyAssociations( visitor );
        }

        public <ThrowableType extends Throwable> void visitProperties( StateVisitor<ThrowableType> visitor )
            throws ThrowableType
        {
            entityPropertiesInstance.visitProperties( visitor );
        }

        public void checkConstraints()
        {
            entityPropertiesInstance.checkConstraints();
            entityAssociationsInstance.checkConstraints();
            entityManyAssociationsInstance.checkConstraints();
        }
    }
}
