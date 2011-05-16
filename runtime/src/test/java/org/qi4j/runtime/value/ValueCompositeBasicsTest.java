package org.qi4j.runtime.value;

import org.junit.Ignore;
import org.junit.Test;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.property.Property;
import org.qi4j.api.value.ValueBuilder;
import org.qi4j.api.value.ValueComposite;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.core.testsupport.AbstractQi4jTest;

import static org.junit.Assert.*;

@Ignore( "Wait for fix for QI-328")
public class ValueCompositeBasicsTest
    extends AbstractQi4jTest
{
    @Override
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.values( SomeValue.class );
    }

    @Test
    public void testEqualsForValueComposite()
    {
        ValueBuilder<SomeValue> builder = valueBuilderFactory.newValueBuilder( SomeValue.class );
        builder.prototypeFor( SomeInternalState.class ).name().set( "Niclas" );
        SomeValue value1 = builder.newInstance();
        SomeValue value2 = builder.newInstance();
        builder.prototypeFor( SomeInternalState.class ).name().set( "Niclas2" );
        SomeValue value3 = builder.newInstance();
        assertEquals( value1, value2 );
        assertFalse( value1.equals( value3 ) );
    }

    @Test
    public void testToStringForValueComposite()
    {
        ValueBuilder<SomeValue> builder = valueBuilderFactory.newValueBuilder( SomeValue.class );
        builder.prototypeFor( SomeInternalState.class ).name().set( "Niclas" );
        SomeValue underTest = builder.newInstance();
        assertEquals( "{name: \"Niclas\"}", underTest.toString() );
    }

    @Test
    public void testToJSonForValueComposite()
    {
        ValueBuilder<SomeValue> builder = valueBuilderFactory.newValueBuilder( SomeValue.class );
        builder.prototypeFor( SomeInternalState.class ).name().set( "Niclas" );
        SomeValue underTest = builder.newInstance();
        assertEquals( "{name: \"Niclas\"}", underTest.toJSON() );
    }

    public abstract static class SomeMixin
        implements SomeValue
    {
        @This
        private SomeInternalState state;

        @Override
        public String name()
        {
            return state.name().get();
        }
    }

    @Mixins( SomeMixin.class )
    public interface SomeValue
        extends ValueComposite
    {
        String name();
    }

    public interface SomeInternalState
    {
        Property<String> name();
    }
}
