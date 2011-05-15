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

import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.qi4j.api.value.ValueComposite;
import org.qi4j.bootstrap.Assembler;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;

import static org.junit.Assert.*;

public class MultipleRunnerTest
    extends AbstractQi4jMultipleRunTest
{
    private String testCase;

    public MultipleRunnerTest( Assembler assembler, String testCase )
    {
        super( assembler );
        this.testCase = testCase;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> configuration()
    {
        ArrayList<Object[]> result = new ArrayList<Object[]>();
        result.add( new Object[]{ new TestAssembler( "Niclas" ), "1" } );
        result.add( new Object[]{ new TestAssembler( "Hedhman" ), "2" } );
        return result;
    }

    @Test
    public void givenValueWhenQueryMetaInfoExpectCorrectValue()
    {
        TestValue value = valueBuilderFactory.newValue( TestValue.class );

        assertTrue( ( "1".equals( testCase ) && value.metaInfo( String.class ).equals( "Niclas" ) ) ||
                    ( "2".equals( testCase ) && value.metaInfo( String.class ).equals( "Hedhman" ) ) );
    }

    public static class TestAssembler
        implements Assembler
    {
        private String value;

        public TestAssembler( String value )
        {

            this.value = value;
        }

        @Override
        public void assemble( ModuleAssembly module )
            throws AssemblyException
        {
            module.values( TestValue.class ).setMetaInfo( value );
        }
    }

    public interface TestValue
        extends ValueComposite
    {
    }
}
