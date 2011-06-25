package org.qi4j.runtime.composite;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.junit.Test;
import org.qi4j.api.composite.TransientComposite;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.core.testsupport.AbstractQi4jTest;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QI247Test2
    extends AbstractQi4jTest
{

    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.transients( TransientWithHandler.class );
    }

    private void checkToString( ObjectMethods instance )
    {
        assertEquals( ObjectMethods.MESSAGE, instance.toString() );
    }

    private void checkHashCode( ObjectMethods instance )
    {
        assertEquals( ObjectMethods.CODE, instance.hashCode() );
    }

    private void checkSelfEquals( ObjectMethods instance )
    {
        assertEquals( instance, instance );
    }

    private void checkTwoNotEqual( ObjectMethods first, ObjectMethods second )
    {
        assertFalse( first.equals( second ) );
    }

    //HANDLER

    @Test
    public void testWithHandlerToString()
    {
        ObjectMethods withHandler = transientBuilderFactory.newTransient( ObjectMethods.class );
        checkToString( withHandler );
    }

    @Test
    public void testWithHandlerHashCode()
    {
        ObjectMethods withHandler = transientBuilderFactory.newTransient( ObjectMethods.class );
        checkHashCode( withHandler );
    }

    @Test
    public void testWithHandlerSelfEquals()
    {
        ObjectMethods withHandler = transientBuilderFactory.newTransient( ObjectMethods.class );
        checkSelfEquals( withHandler );
    }

    @Test
    public void testWithHandlerSelfEquals2()
    {
        ObjectMethods withHandler = transientBuilderFactory.newTransient( ObjectMethods.class );
        assertTrue( withHandler.equals( withHandler ) );
    }

    @Test
    public void testWithHandlerSelfSame()
    {
        ObjectMethods withHandler = transientBuilderFactory.newTransient( ObjectMethods.class );
        assertSame( withHandler, withHandler );
    }

    @Test
    public void testWithHandlerTwoNotEqual()
    {
        ObjectMethods first = transientBuilderFactory.newTransient( ObjectMethods.class );
        ObjectMethods second = transientBuilderFactory.newTransient( ObjectMethods.class );
        checkTwoNotEqual( first, second );
    }

    public interface ObjectMethods
    {
        String MESSAGE = "Does not work :(";

        int CODE = 123;

        void someMethod();
    }

    public static class ObjectMethodsHandler
        implements InvocationHandler
    {
        public Object invoke( Object proxy, Method method, Object[] args )
            throws Throwable
        {
            System.out.println( "invoke(proxy, " + method.getName() + ", args" );
            if( "someMethod".equals( method.getName() ) )
            {
                System.out.println( "Hello." );
                return null;
            }
            else
            {
                throw new UnsupportedOperationException( method.toString() );
            }
        }

        public String toString()
        {
            return ObjectMethods.MESSAGE;
        }

        public int hashCode()
        {
            return ObjectMethods.CODE;
        }

        public boolean equals( Object o )
        {
            return o == this;
        }
    }

    @Mixins( ObjectMethodsHandler.class )
    public interface TransientWithHandler
        extends TransientComposite, ObjectMethods
    {
    }
}
