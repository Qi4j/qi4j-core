/*
 * Copyright (c) 2007, Rickard Öberg. All Rights Reserved.
 * Copyright (c) 2007, Niclas Hedhman. All Rights Reserved.
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
package org.qi4j.api.composite;

import org.qi4j.api.common.ConstructionException;

/**
 * This factory creates proxies that implement the given
 * thisAs interfaces.
 */
public interface CompositeBuilderFactory
{
    /**
     * Create a builder for creating new Composites that implements the given Composite type.
     *
     * @param mixinType an interface that describes the Composite to be instantiated
     * @return a CompositeBuilder for creation of Composites implementing the interface
     * @throws NoSuchCompositeException if no composite extending the mixinType has been registered
     */
    <T> CompositeBuilder<T> newCompositeBuilder( Class<T> mixinType )
        throws NoSuchCompositeException;

    /**
     * Instantiate a Composite of the given type.
     *
     * @param mixinType the Composite type to instantiate
     * @return a new Composite instance
     * @throws NoSuchCompositeException if no composite extending the mixinType has been registered
     * @throws org.qi4j.api.common.ConstructionException if the composite could not be instantiated
     */
    <T> T newComposite( Class<T> mixinType )
        throws NoSuchCompositeException, ConstructionException;
}
