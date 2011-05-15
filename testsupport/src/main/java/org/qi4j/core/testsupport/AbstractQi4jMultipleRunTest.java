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
 *
 */
package org.qi4j.core.testsupport;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.qi4j.bootstrap.ApplicationAssembler;
import org.qi4j.bootstrap.Assembler;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.test.AbstractQi4jTest;

/**
 * This abstract test helper is for running the same set of tests but with different Assemblers.
 * <p>
 * To use it, you need to provide a {@code &#64;Parameters} annotated method in your test class, which
 * returns a Collection of an array of arguments for your constructor.
 * </p>
 * <p>
 * Your constructor should either call this class' constructor of {@link Assembler} or {@link ApplicationAssembler}.
 * </p>
 * <p>
 * Example;
 * </p>
 * <code><pre>
 * public class MyTest
 *     extends AbstractQi4jMultipleRunTest
 * {
 *
 *     private String someValue;
 *
 *     public MyTest( Assembler assembler, String value )
 *     {
 *         super( assembler );
 *         this.someValue = value;
 *     }
 *
 *     &#64;Parameterized.Parameters
 *     public static Collection&lt;Object[]&gt; configuration()
 *     {
 *         ArrayList&lt;Object[]&gt; result = new ArrayList&lt;Object[]&gt;();
 *         result.add( new Object[]{ new TestAssembler( "Niclas" ), "1" } );
 *         result.add( new Object[]{ new TestAssembler( "Hedhman" ), "2" } );
 *         return result;
 *     }
 *
 *     &#64;Test
 *     public void ....
 *
 * </pre></code>
 */
@RunWith( Parameterized.class )
public class AbstractQi4jMultipleRunTest
    extends AbstractQi4jTest
{

    private Assembler moduleAssembler;
    private ApplicationAssembler applicationAssembler;

    protected AbstractQi4jMultipleRunTest( ApplicationAssembler assembler )
    {
        applicationAssembler = assembler;
    }

    protected AbstractQi4jMultipleRunTest( Assembler assembler )
    {
        moduleAssembler = assembler;
    }

    @Override
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        if( moduleAssembler != null )
        {
            moduleAssembler.assemble( module );
        }
    }

    @Override
    protected ApplicationAssembler createApplicationAssembler()
    {
        return applicationAssembler;
    }
}
