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

package org.qi4j.spi.entity.association;

import org.qi4j.api.entity.EntityReference;

public class NamedEntityReference
{
    private String name;
    private EntityReference entity;

    public NamedEntityReference( String name, EntityReference entity )
    {
        this.name = name;
        this.entity = entity;
    }

    public String name()
    {
        return name;
    }

    public EntityReference entityReference()
    {
        return entity;
    }
}
