package org.qi4j.regression.qi55;

import org.junit.Test;
import org.qi4j.api.injection.scope.Uses;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.core.testsupport.AbstractQi4jTest;

import static junit.framework.Assert.*;

public class IssueTest
    extends AbstractQi4jTest
{
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.objects( AClass.class );
    }

    @Test
    public void objectWithGenericUsage()
    {
        assertEquals( "Using - Test string", objectBuilderFactory.
            newObjectBuilder( AClass.class ).
            use( "Test string" ).
            newInstance().
            uses() );
    }

    public static class AClass<T>
    {
        @Uses
        T value;

        public String uses()
        {
            return "Using - " + value;
        }
    }
}