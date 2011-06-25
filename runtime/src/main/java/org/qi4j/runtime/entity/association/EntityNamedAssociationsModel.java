/*
 * Copyright (c) 2008, Rickard Öberg. All Rights Reserved.
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

package org.qi4j.runtime.entity.association;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.qi4j.api.common.MetaInfo;
import org.qi4j.api.common.Optional;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.entity.association.GenericAssociationInfo;
import org.qi4j.api.entity.association.NamedAssociation;
import org.qi4j.api.util.Annotations;
import org.qi4j.bootstrap.NamedAssociationDeclarations;
import org.qi4j.runtime.composite.ConstraintsModel;
import org.qi4j.runtime.composite.ValueConstraintsInstance;
import org.qi4j.runtime.composite.ValueConstraintsModel;
import org.qi4j.runtime.structure.ModuleUnitOfWork;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.association.AssociationDescriptor;
import org.qi4j.spi.entity.association.AssociationType;
import org.qi4j.spi.util.MethodKeyMap;
import org.qi4j.spi.util.MethodSet;
import org.qi4j.spi.util.MethodValueMap;

import static org.qi4j.api.util.Annotations.*;
import static org.qi4j.api.util.Iterables.*;

/**
 * JAVADOC
 */
public final class EntityNamedAssociationsModel
    implements Serializable
{
    private final Set<Method> methods = new MethodSet();
    private final Set<NamedAssociationModel> namedAssociationModels = new LinkedHashSet<NamedAssociationModel>();
    private final Map<Method, NamedAssociationModel> mapMethodAssociationModel = new MethodKeyMap<NamedAssociationModel>();
    private final Map<QualifiedName, Method> accessors = new MethodValueMap<QualifiedName>();
    private final ConstraintsModel constraints;
    private final NamedAssociationDeclarations namedAssociationDeclarations;

    public EntityNamedAssociationsModel( ConstraintsModel constraints,
                                         NamedAssociationDeclarations namedAssociationDeclarations
    )
    {
        this.constraints = constraints;
        this.namedAssociationDeclarations = namedAssociationDeclarations;
    }

    public void addNamedAssociationFor( Method method )
    {
        if( !methods.contains( method ) )
        {
            if( NamedAssociation.class.isAssignableFrom( method.getReturnType() ) )
            {
                Iterable<Annotation> annotations = Annotations.getMethodAndTypeAnnotations( method );
                boolean optional = first( filter( isType( Optional.class ), annotations ) ) != null;

                // Constraints for entities in NamedAssociation
                ValueConstraintsModel valueConstraintsModel = constraints.constraintsFor( annotations, GenericAssociationInfo
                    .getAssociationType( method ), method.getName(), optional );
                ValueConstraintsInstance valueConstraintsInstance = null;
                if( valueConstraintsModel.isConstrained() )
                {
                    valueConstraintsInstance = valueConstraintsModel.newInstance();
                }

                // Constraints for the NamedAssociation itself
                valueConstraintsModel = constraints.constraintsFor( annotations, NamedAssociation.class, method.getName(), optional );
                ValueConstraintsInstance namedValueConstraintsInstance = null;
                if( valueConstraintsModel.isConstrained() )
                {
                    namedValueConstraintsInstance = valueConstraintsModel.newInstance();
                }
                MetaInfo metaInfo = namedAssociationDeclarations.getMetaInfo( method );
                NamedAssociationModel associationModel = new NamedAssociationModel( method, valueConstraintsInstance, namedValueConstraintsInstance, metaInfo );
                if( !accessors.containsKey( associationModel.qualifiedName() ) )
                {
                    namedAssociationModels.add( associationModel );
                    mapMethodAssociationModel.put( method, associationModel );
                    accessors.put( associationModel.qualifiedName(), associationModel.accessor() );
                }
            }
            methods.add( method );
        }
    }

    public <T extends AssociationDescriptor> Set<T> namedAssociations()
    {
        return (Set<T>) namedAssociationModels;
    }

    public <T> NamedAssociation<T> newInstance( Method accessor, EntityState entityState, ModuleUnitOfWork uow )
    {
        NamedAssociationModel model = mapMethodAssociationModel.get( accessor );
        return model.newInstance( uow, entityState );
    }

    public AssociationDescriptor getNamedAssociationByName( String name )
    {
        for( NamedAssociationModel associationModel : namedAssociationModels )
        {
            if( associationModel.qualifiedName().name().equals( name ) )
            {
                return associationModel;
            }
        }

        return null;
    }

    public Set<AssociationType> namedAssociationTypes()
    {
        Set<AssociationType> associationTypes = new LinkedHashSet<AssociationType>();
        for( NamedAssociationModel associationModel : namedAssociationModels )
        {
            associationTypes.add( associationModel.associationType() );
        }
        return associationTypes;
    }

    public void checkConstraints( EntityNamedAssociationsInstance entityNamedAssociationsInstance )
    {
        for( NamedAssociationModel namedAssociationModel : namedAssociationModels )
        {
            NamedAssociation namedAssociation = entityNamedAssociationsInstance.namedAssociationFor( namedAssociationModel
                                                                                                         .accessor() );
            namedAssociationModel.checkAssociationConstraints( namedAssociation );
        }
    }

    public EntityNamedAssociationsInstance newInstance( EntityState entityState, ModuleUnitOfWork uow )
    {
        return new EntityNamedAssociationsInstance( this, entityState, uow );
    }
}