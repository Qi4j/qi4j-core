package org.qi4j.test.entity;

import org.junit.Test;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.composite.CompositeBuilder;
import org.qi4j.entity.EntityComposite;
import org.qi4j.entity.UnitOfWork;
import org.qi4j.entity.UnitOfWorkCompletionException;
import org.qi4j.entity.association.Association;
import org.qi4j.entity.association.ListAssociation;
import org.qi4j.entity.association.ManyAssociation;
import org.qi4j.entity.association.SetAssociation;
import org.qi4j.property.Property;
import org.qi4j.spi.entity.UuidIdentityGeneratorComposite;
import org.qi4j.test.AbstractQi4jTest;

/**
 * Test for MemoryEntityStoreComposite
 */
public abstract class AbstractEntityStorePerformanceTest
    extends AbstractQi4jTest
{
    protected int nrOfEntities = 10000;
    protected int nrOfLookups = 100000;

    public void assemble( ModuleAssembly module ) throws AssemblyException
    {
        module.addServices( UuidIdentityGeneratorComposite.class );
        module.addComposites( TestComposite.class );
    }

    @Test
    public void whenNewEntitiesThenPerformanceIsOk()
        throws Exception
    {
        long start = System.currentTimeMillis();
        for( int i = 0; i < nrOfEntities; i++ )
        {
            UnitOfWork unitOfWork = unitOfWorkFactory.newUnitOfWork();
            createEntity( unitOfWork );
            unitOfWork.complete();
        }
        long end = System.currentTimeMillis();

        long time = end - start;
        System.out.println( time );
        System.out.println( nrOfEntities / ( time / 1000.0D ) + " entities created per second" );
    }

    @Test
    public void whenBulkNewEntitiesThenPerformanceIsOk()
        throws Exception
    {
        UnitOfWork unitOfWork = unitOfWorkFactory.newUnitOfWork();

        long start = System.currentTimeMillis();
        CompositeBuilder<TestComposite> builder = unitOfWork.newEntityBuilder( TestComposite.class );
        for( int i = 0; i < nrOfEntities; i++ )
        {
            // Create entity
            builder.newInstance();
        }

        unitOfWork.complete();
        long end = System.currentTimeMillis();
        long time = end - start;
        System.out.println( time );
        System.out.println( nrOfEntities / ( time / 1000.0D ) + " entities created per second" );
    }

    @Test
    public void whenFindEntityThenPerformanceIsOk()
        throws Exception
    {
        long start = System.currentTimeMillis();

        UnitOfWork unitOfWork = unitOfWorkFactory.newUnitOfWork();
        String id = createEntity( unitOfWork ).identity().get();
        unitOfWork.complete();

        unitOfWork = unitOfWorkFactory.newUnitOfWork();
        for( int i = 0; i < nrOfLookups; i++ )
        {
            TestComposite instance = unitOfWork.find( id, TestComposite.class );
            unitOfWork.clear();
        }
        unitOfWork.discard();

        long end = System.currentTimeMillis();
        long time = end - start;
        System.out.println( time );
        System.out.println( nrOfLookups / ( time / 1000.0D ) + " lookups per second" );
    }

    protected TestComposite createEntity( UnitOfWork unitOfWork )
        throws UnitOfWorkCompletionException
    {
        // Create entity
        CompositeBuilder<TestComposite> builder = unitOfWork.newEntityBuilder( TestComposite.class );
        TestComposite instance = builder.newInstance();
        String id = instance.identity().get();

        instance.name().set( "Test" );
        instance.association().set( instance );

        instance.manyAssociation().add( instance );

        instance.listAssociation().add( instance );
        instance.listAssociation().add( instance );
        instance.listAssociation().add( instance );

        instance.setAssociation().add( instance );
        instance.setAssociation().add( instance );
        return instance;
    }

    public interface TestComposite
        extends EntityComposite
    {
        Property<String> name();

        Property<String> unsetName();

        Association<TestComposite> association();

        Association<TestComposite> unsetAssociation();

        ManyAssociation<TestComposite> manyAssociation();

        ListAssociation<TestComposite> listAssociation();

        SetAssociation<TestComposite> setAssociation();
    }
}