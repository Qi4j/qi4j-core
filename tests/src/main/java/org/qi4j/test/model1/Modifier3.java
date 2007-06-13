/*  Copyright 2007 Niclas Hedhman.
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
 */
package org.qi4j.test.model1;

import org.qi4j.api.annotation.Modifies;

public class Modifier3
    implements Mixin1, Mixin2
{
    @Modifies Mixin1 mixin1;
    @Modifies Mixin2 mixin2;

    public String do1()
    {
        return mixin2 + "-m3";
    }

    public String do2()
    {
        return mixin1 + "-m3";
    }
}