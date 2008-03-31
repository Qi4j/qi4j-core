/*
 * Copyright 2008 Alin Dreghiciu.
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
 *
 */
package org.qi4j.query.grammar;

/**
 * An expression related to {@link org.qi4j.property.Property}.
 */
public interface PropertyReference<T>
{

    /**
     * Get the name of the property, which is equal to the name of the method that declared it.
     *
     * @return the name of the property
     */
    String getName();

    /**
     * Get the type of the interface that declared the property.
     *
     * @return the type of property that declared the property
     */
    Class getDeclaringType();

    /**
     * Get the type of the property. If the property is declared as Property<X> then X is returned.
     *
     * @return the property type
     */
    Class<T> getType();

    /**
     * Gets the traversed association used to get to this property  or null if there was no traversal involved.
     *
     * @return traversed association used to get to this property.
     */
    AssociationReference getTraversedAssociation();

}