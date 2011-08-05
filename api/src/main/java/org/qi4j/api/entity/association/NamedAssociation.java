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

package org.qi4j.api.entity.association;

import java.util.Map;

/**
 * A NamedAssociation can be looked-up in its local context (the set of associations) via a name (java.lang.String).
 *
 * <p>
 * The Iterable&lt;String&ht; returns the names in the association set.
 * </p>
 *
 * @param <T> The type of the NamedAssociation.
 */
public interface NamedAssociation<T>
    extends AbstractAssociation, Iterable<String>
{

    /**
     * Adds an assocation.
     *
     * @param name   The name of the associated entity.
     * @param entity The entity for this named association.
     */
    void put( String name, T entity );

    /**
     * Retrieves a named association.
     *
     * @param name The name of the association.
     *
     * @return The entity that has previously been associated.
     */
    T get( String name );

    /**
     * @return The number of named associations in this NamedAssociation (i.e. local context).
     */
    int count();

    /**
     * Checks if there is an association with the given name.
     *
     * @param name The name of the association we are checking if it exists.
     *
     * @return true if it exists, false otherwise.
     */
    boolean containsKey( String name );

    /**
     * Checks if the entity is present.
     * Note that this is potentially a very slow operation, depending on the size of the {@code NamedAssociation}.
     *
     * @param entity The entity to look for.
     *
     * @return The name of the entity if found, otherwise {@code null}.
     */
    String contains( T entity );

    /**
     * Returns the names in this set of association.
     *
     * @return The names of the associations that has been entered already.
     */
    Iterable<String> names();

    /**
     * Converts this {@code NamedAssociation} into a standard Java Map.
     *
     * @return A fully populated {@link java.util.Map} with the content of this {@code NamedAssociation}.
     */
    Map<String, T> toMap();
}
