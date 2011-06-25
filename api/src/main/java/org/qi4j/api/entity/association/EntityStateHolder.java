/*
 * Copyright 2008 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.api.entity.association;

import java.lang.reflect.Method;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.property.StateHolder;

/**
 * This represents the state of a entity (properties+associations).
 */
public interface EntityStateHolder
    extends StateHolder
{
    /**
     * Get an association for a specific accessor method
     *
     * @param associationMethod for the association
     *
     * @return the association
     */
    <T> Association<T> getAssociation( Method associationMethod );

    /**
     * Get a many-association for a specific accessor method
     *
     * @param manyassociationMethod for the many-association
     *
     * @return the association
     */
    <T> ManyAssociation<T> getManyAssociation( Method manyassociationMethod );

    /**
     * Get a named-association for a specific accessor method
     *
     * @param namedassociationMethod for the many-association
     *
     * @return the association
     */
    <T> NamedAssociation<T> getNamedAssociation( Method namedassociationMethod );

    <ThrowableType extends Throwable> void visitState( EntityStateVisitor<ThrowableType> visitor )
        throws ThrowableType;

    public interface EntityStateVisitor<ThrowableType extends Throwable>
        extends StateVisitor<ThrowableType>
    {
        void visitAssociation( QualifiedName name, Object association )
            throws ThrowableType;

        void visitManyAssociation( QualifiedName name, ManyAssociation association )
            throws ThrowableType;

        void visitNamedAssociation( QualifiedName name, NamedAssociation association )
            throws ThrowableType;
    }
}
