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
package org.qi4j.api.mixin;

/**
 * Thrown when a Fragment or object could not be instantiated.
 */
public class InitializationException
    extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public InitializationException()
    {
    }

    public InitializationException( String message )
    {
        super( message );
    }

    public InitializationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public InitializationException( Throwable cause )
    {
        super( cause );
    }
}
