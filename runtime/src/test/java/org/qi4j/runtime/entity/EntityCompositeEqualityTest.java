package org.qi4j.runtime.entity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qi4j.api.common.Visibility;
import org.qi4j.api.entity.EntityBuilder;
import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkCompletionException;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.core.testsupport.AbstractQi4jTest;
import org.qi4j.entitystore.memory.MemoryEntityStoreService;
import org.qi4j.spi.uuid.UuidIdentityGeneratorService;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Testing of equality of entity composites.
 */
public class EntityCompositeEqualityTest
    extends AbstractQi4jTest
{
    private UnitOfWork unitOfWork;
    private EntityBuilder<MyComposite> myCompositeBuilder;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        unitOfWork = this.unitOfWorkFactory.newUnitOfWork();
        myCompositeBuilder = unitOfWork.newEntityBuilder( MyComposite.class );
    }

    @After
    public void completeUnitOfWork()
        throws Exception
    {
        unitOfWork.complete();
        super.tearDown();
    }

    @Test
    public void shouldNotBeEqualToNull()
        throws UnitOfWorkCompletionException
    {
        MyComposite simpleComposite = myCompositeBuilder.newInstance();
        assertThat( "simpleComposite is not equal to null", simpleComposite.equals( null ), equalTo( false ) );
    }

    @Test
    public void shouldBeEqualToItself()
    {
        MyComposite simpleComposite = myCompositeBuilder.newInstance();
        assertThat( "simple composite is equal to itself", simpleComposite.equals( simpleComposite ), equalTo( true ) );
    }

    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.services( UuidIdentityGeneratorService.class, MemoryEntityStoreService.class )
            .visibleIn( Visibility.layer );
        module.objects( EntityCompositeEqualityTest.class );
        module.entities( MyComposite.class );
    }

    private static interface MyComposite
        extends EntityComposite
    {
    }
}
