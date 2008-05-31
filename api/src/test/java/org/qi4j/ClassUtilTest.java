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

package org.qi4j;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.qi4j.util.ClassUtil.interfacesOf;

/**
 * Tests for ClassUtil
 */
public class ClassUtilTest
{
    @Test
    public void givenClassWithInterfacesWhenInterfacesOfThenGetCorrectSet()
    {
        assertThat( "one interface returned", interfacesOf( A.class ).size(), equalTo( 1 ) );
        assertThat( "two interface returned", interfacesOf( B.class ).size(), equalTo( 2 ) );
        assertThat( "tree interface returned", interfacesOf( C.class ).size(), equalTo( 3 ) );
    }

    @Test
    public void givenClassWithInterfacesWhenGetInterfacesWithMethodsThenGetCorrectSet()
    {
        assertThat( "one interface returned", interfacesOf( C.class ).size(), equalTo( 1 ) );
    }

    interface A
    {
    }

    interface B extends A
    {
        public void doStuff();
    }

    interface C extends A, B
    {
    }
}
