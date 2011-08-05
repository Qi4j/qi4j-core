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

package org.qi4j.runtime.structure;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import org.json.JSONTokener;
import org.qi4j.api.Qi4j;
import org.qi4j.api.common.ConstructionException;
import org.qi4j.api.common.Visibility;
import org.qi4j.api.composite.AmbiguousTypeException;
import org.qi4j.api.composite.NoSuchCompositeException;
import org.qi4j.api.composite.TransientBuilder;
import org.qi4j.api.composite.TransientBuilderFactory;
import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.object.NoSuchObjectException;
import org.qi4j.api.object.ObjectBuilder;
import org.qi4j.api.object.ObjectBuilderFactory;
import org.qi4j.api.property.StateHolder;
import org.qi4j.api.query.QueryBuilderFactory;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.service.ServiceFinder;
import org.qi4j.api.service.ServiceReference;
import org.qi4j.api.structure.Module;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;
import org.qi4j.api.usecase.Usecase;
import org.qi4j.api.util.NullArgumentException;
import org.qi4j.api.value.NoSuchValueException;
import org.qi4j.api.value.ValueBuilder;
import org.qi4j.api.value.ValueBuilderFactory;
import org.qi4j.runtime.composite.TransientBuilderInstance;
import org.qi4j.runtime.composite.TransientModel;
import org.qi4j.runtime.composite.TransientsModel;
import org.qi4j.runtime.composite.UsesInstance;
import org.qi4j.runtime.entity.EntitiesInstance;
import org.qi4j.runtime.entity.EntitiesModel;
import org.qi4j.runtime.entity.EntityInstance;
import org.qi4j.runtime.injection.InjectionContext;
import org.qi4j.runtime.object.ObjectBuilderInstance;
import org.qi4j.runtime.object.ObjectModel;
import org.qi4j.runtime.object.ObjectsModel;
import org.qi4j.runtime.query.QueryBuilderFactoryImpl;
import org.qi4j.runtime.service.ImportedServicesInstance;
import org.qi4j.runtime.service.ImportedServicesModel;
import org.qi4j.runtime.service.ServicesInstance;
import org.qi4j.runtime.service.ServicesModel;
import org.qi4j.runtime.unitofwork.UnitOfWorkInstance;
import org.qi4j.runtime.value.ValueBuilderInstance;
import org.qi4j.runtime.value.ValueModel;
import org.qi4j.runtime.value.ValuesModel;
import org.qi4j.spi.composite.TransientDescriptor;
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.object.ObjectDescriptor;
import org.qi4j.spi.structure.ModuleSPI;
import org.qi4j.spi.value.ValueDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instance of a Qi4j Module. Contains the various composites for this Module.
 */
public class ModuleInstance
    implements Module, ModuleSPI, Activatable
{
    private static final Logger logger = LoggerFactory.getLogger( Qi4j.class );

    private final ModuleModel moduleModel;
    private final LayerInstance layerInstance;
    private final EntitiesInstance entities;
    private final ServicesInstance services;
    private final ImportedServicesInstance importedServices;

    private final TransientBuilderFactory transientBuilderFactory;
    private final ObjectBuilderFactory objectBuilderFactory;
    private final ValueBuilderFactory valueBuilderFactory;
    private final UnitOfWorkFactory unitOfWorkFactory;
    private final QueryBuilderFactory queryBuilderFactory;
    private final ServiceFinder serviceFinder;

    // Lookup caches
    private final Map<Class, EntityFinder> entityFinders;
    private final Map<Class, CompositeFinder> compositeFinders;
    private final Map<Class, ObjectFinder> objectFinders;
    private final Map<Class, ValueFinder> valueFinders;

    public ModuleInstance( ModuleModel moduleModel, LayerInstance layerInstance, TransientsModel transientsModel,
                           EntitiesModel entitiesModel, ObjectsModel objectsModel, ValuesModel valuesModel,
                           ServicesModel servicesModel, ImportedServicesModel importedServicesModel
    )
    {
        this.moduleModel = moduleModel;
        this.layerInstance = layerInstance;
        entities = new EntitiesInstance( entitiesModel, this );
        services = servicesModel.newInstance( this );
        importedServices = importedServicesModel.newInstance( this );

        transientBuilderFactory = new TransientBuilderFactoryInstance();
        objectBuilderFactory = new ObjectBuilderFactoryInstance();
        valueBuilderFactory = new ValueBuilderFactoryInstance();
        unitOfWorkFactory = new UnitOfWorkFactoryInstance();
        serviceFinder = new ServiceFinderInstance();
        queryBuilderFactory = new QueryBuilderFactoryImpl( serviceFinder );

        entityFinders = new ConcurrentHashMap<Class, EntityFinder>();
        compositeFinders = new ConcurrentHashMap<Class, CompositeFinder>();
        objectFinders = new ConcurrentHashMap<Class, ObjectFinder>();
        valueFinders = new ConcurrentHashMap<Class, ValueFinder>();
    }

    public String name()
    {
        return moduleModel.name();
    }

    public <T> T metaInfo( Class<T> infoType )
    {
        return moduleModel.metaInfo( infoType );
    }

    public ModuleModel model()
    {
        return moduleModel;
    }

    public LayerInstance layerInstance()
    {
        return layerInstance;
    }

    public EntitiesInstance entities()
    {
        return entities;
    }

    public ServicesInstance services()
    {
        return services;
    }

    public ImportedServicesInstance importedServices()
    {
        return importedServices;
    }

    public EntityDescriptor entityDescriptor( String name )
    {
        try
        {
            EntityFinder finder = findEntityModel( classLoader().loadClass( name ) );
            if( finder.noModelExist() )
            {
                return null;
            }
            return finder.getFoundModel();
        }
        catch( ClassNotFoundException e )
        {
            return null;
        }
    }

    public ObjectDescriptor objectDescriptor( String typeName )
    {
        try
        {
            ObjectFinder finder = findObjectModel( classLoader().loadClass( typeName ) );
            return finder.model;
        }
        catch( ClassNotFoundException e )
        {
            return null;
        }
    }

    public TransientDescriptor transientDescriptor( String name )
    {
        try
        {
            CompositeFinder finder = findTransientModel( classLoader().loadClass( name ) );
            return finder.model;
        }
        catch( ClassNotFoundException e )
        {
            return null;
        }
    }

    public ValueDescriptor valueDescriptor( String name )
    {
        try
        {
            ValueFinder finder = findValueModel( classLoader().loadClass( name ) );
            return finder.model;
        }
        catch( ClassNotFoundException e )
        {
            return null;
        }
    }

    public TransientBuilderFactory transientBuilderFactory()
    {
        return transientBuilderFactory;
    }

    public ObjectBuilderFactory objectBuilderFactory()
    {
        return objectBuilderFactory;
    }

    public ValueBuilderFactory valueBuilderFactory()
    {
        return valueBuilderFactory;
    }

    public UnitOfWorkFactory unitOfWorkFactory()
    {
        return unitOfWorkFactory;
    }

    public QueryBuilderFactory queryBuilderFactory()
    {
        return queryBuilderFactory;
    }

    public ServiceFinder serviceFinder()
    {
        return serviceFinder;
    }

    public ClassLoader classLoader()
    {
        return moduleModel.classLoader();
    }

    public <ThrowableType extends Throwable> void visitModules( ModuleVisitor<ThrowableType> visitor )
        throws ThrowableType
    {
        // Visit this module
        if( !visitor.visitModule( this, moduleModel, Visibility.module ) )
        {
            return;
        }

        // Visit layer
        layerInstance.visitModules( visitor, Visibility.layer );
    }

    public void activate()
        throws Exception
    {
        services.activate();

        logger.debug( "Module " + name() + " activated" );
    }

    public void passivate()
        throws Exception
    {
        services.passivate();

        logger.debug( "Module " + name() + " passivated" );
    }

    @Override
    public String toString()
    {
        return moduleModel.toString();
    }

    private <T> ServiceReference<T> getServiceFor( Type type, Visibility visibility )
    {
        ServiceReference<T> service;
        service = services.getServiceFor( type, visibility );
        if( service == null )
        {
            service = importedServices.getServiceFor( type, visibility );
        }

        return service;
    }

    private <T> void getServicesFor( Type type, Visibility visibility, List<ServiceReference<T>> serviceReferences )
    {
        services.getServicesFor( type, visibility, serviceReferences );
        importedServices.getServicesFor( type, visibility, serviceReferences );
    }

    public EntityFinder findEntityModel( Class type )
    {
        EntityFinder finder = entityFinders.get( type );
        if( finder == null )
        {
            finder = new EntityFinder( type );
            visitModules( finder );
            entityFinders.put( type, finder );
        }
        return finder;
    }

    public CompositeFinder findTransientModel( Class mixinType )
    {
        CompositeFinder finder = compositeFinders.get( mixinType );
        if( finder == null )
        {
            finder = new CompositeFinder();
            finder.type = mixinType;
            visitModules( finder );
            if( finder.model != null )
            {
                compositeFinders.put( mixinType, finder );
            }
        }

        return finder;
    }

    public ObjectFinder findObjectModel( Class type )
    {
        ObjectFinder finder = objectFinders.get( type );
        if( finder == null )
        {
            finder = new ObjectFinder();
            finder.type = type;
            visitModules( finder );
            if( finder.model != null )
            {
                objectFinders.put( type, finder );
            }
        }

        return finder;
    }

    private ValueFinder findValueModel( Class type )
    {
        ValueFinder finder = valueFinders.get( type );
        if( finder == null )
        {
            finder = new ValueFinder();
            finder.type = type;
            visitModules( finder );
            if( finder.model != null )
            {
                valueFinders.put( type, finder );
            }
        }

        return finder;
    }

    private abstract class TypeFinder<T extends ObjectDescriptor>
        implements ModuleVisitor<RuntimeException>
    {
        public Class type;

        public T model;
        public ModuleInstance module;
        public Visibility visibility;

        public boolean visitModule( ModuleInstance moduleInstance, ModuleModel moduleModel, Visibility visibility )
        {
            T foundModel = findModel( moduleModel, visibility );
            if( foundModel != null )
            {
                if( model == null )
                {
                    model = foundModel;
                    module = moduleInstance;
                    this.visibility = visibility;
                }
                else
                {
                    // If same visibility -> ambiguous types
                    if( this.visibility == visibility )
                    {
                        // Check if they are the same type
                        if( model.type().equals( foundModel.type() ) )
                        {
                            // Same type, same scope -> ambiguous
                            throw new AmbiguousTypeException( type );
                        }
                        else
                        {
                            // If any type is an exact match, use it
                            if( model.type().equals( type ) )
                            {
                                // Do nothing
                            }
                            else if( foundModel.type().equals( type ) )
                            {
                                // Use this model instead
                                model = foundModel;
                                module = moduleInstance;
                                this.visibility = visibility;
                            }
                            else
                            {
                                // Both types match, none are exact, same scope -> ambiguous
                                throw new AmbiguousTypeException( type );
                            }
                        }
                    }
                }
            }
            else
            {
            }

            // Break if we have found a model and visibility has changed since the find
            return !( model != null && this.visibility != visibility );
        }

        protected abstract T findModel( ModuleModel model, Visibility visibility );
    }

    private class TransientBuilderFactoryInstance
        implements TransientBuilderFactory
    {
        public <T> TransientBuilder<T> newTransientBuilder( Class<T> mixinType )
            throws NoSuchCompositeException
        {
            NullArgumentException.validateNotNull( "mixinType", mixinType );
            CompositeFinder finder = findTransientModel( mixinType );

            if( finder.model == null )
            {
                throw new NoSuchCompositeException( mixinType.getName(), name() );
            }

            return new TransientBuilderInstance<T>( finder.module, finder.model );
        }

        public <T> T newTransient( final Class<T> mixinType )
            throws NoSuchCompositeException, ConstructionException
        {
            NullArgumentException.validateNotNull( "mixinType", mixinType );
            CompositeFinder finder = findTransientModel( mixinType );

            if( finder.model == null )
            {
                throw new NoSuchCompositeException( mixinType.getName(), name() );
            }

            StateHolder stateHolder = finder.model.newInitialState();
            finder.model.state().checkConstraints( stateHolder );
            return finder.model.newCompositeInstance( finder.module, UsesInstance.EMPTY_USES, stateHolder ).<T>proxy();
        }
    }

    public class CompositeFinder
        extends TypeFinder<TransientModel>
    {
        protected TransientModel findModel( ModuleModel model, Visibility visibility )
        {
            return model.composites().getCompositeModelFor( type, visibility );
        }
    }

    private class ObjectBuilderFactoryInstance
        implements ObjectBuilderFactory
    {
        public <T> ObjectBuilder<T> newObjectBuilder( Class<T> mixinType )
            throws NoSuchObjectException
        {
            NullArgumentException.validateNotNull( "mixinType", mixinType );
            ObjectFinder finder = findObjectModel( mixinType );

            if( finder.model == null )
            {
                throw new NoSuchObjectException( mixinType.getName(), name() );
            }
            InjectionContext injectionContext = new InjectionContext( finder.module, UsesInstance.EMPTY_USES );
            return new ObjectBuilderInstance<T>( injectionContext, finder.model );
        }

        public <T> T newObject( Class<T> mixinType )
            throws NoSuchObjectException
        {
            NullArgumentException.validateNotNull( "mixinType", mixinType );
            ObjectFinder finder = findObjectModel( mixinType );

            if( finder.model == null )
            {
                throw new NoSuchObjectException( mixinType.getName(), name() );
            }

            InjectionContext injectionContext = new InjectionContext( finder.module, UsesInstance.EMPTY_USES );
            return mixinType.cast( finder.model.newInstance( injectionContext ) );
        }
    }

    public class ObjectFinder
        extends TypeFinder<ObjectModel>
    {
        protected ObjectModel findModel( ModuleModel model, Visibility visibility )
        {
            return model.objects().getObjectModelFor( type, visibility );
        }
    }

    private class ValueBuilderFactoryInstance
        implements ValueBuilderFactory
    {
        public <T> ValueBuilder<T> newValueBuilder( Class<T> mixinType )
            throws NoSuchValueException
        {
            NullArgumentException.validateNotNull( "mixinType", mixinType );
            ValueFinder finder = findValueModel( mixinType );

            if( finder.model == null )
            {
                throw new NoSuchValueException( mixinType.getName(), name() );
            }

            return new ValueBuilderInstance<T>( finder.module, finder.model );
        }

        public <T> T newValue( Class<T> mixinType )
            throws NoSuchValueException, ConstructionException
        {
            NullArgumentException.validateNotNull( "mixinType", mixinType );
            ValueFinder finder = findValueModel( mixinType );

            if( finder.model == null )
            {
                throw new NoSuchValueException( mixinType.getName(), name() );
            }

            StateHolder initialState = finder.model.newInitialState();
            finder.model.checkConstraints( initialState );
            return mixinType.cast( finder.model.newValueInstance( finder.module, initialState ).proxy() );
        }

        public <T> T newValueFromJSON( Class<T> mixinType, String jsonValue )
            throws NoSuchValueException, ConstructionException
        {
            NullArgumentException.validateNotNull( "mixinType", mixinType );
            ValueFinder finder = findValueModel( mixinType );

            if( finder.model == null )
            {
                throw new NoSuchValueException( mixinType.getName(), name() );
            }

            try
            {
                return (T) finder.model.valueType().fromJSON( new JSONTokener( jsonValue ).nextValue(), finder.module );
            }
            catch( JSONException e )
            {
                throw new ConstructionException( "Could not create value from JSON", e );
            }
        }
    }

    private class ValueFinder
        extends TypeFinder<ValueModel>
    {
        protected ValueModel findModel( ModuleModel model, Visibility visibility )
        {
            return model.values().getValueModelFor( type, visibility );
        }
    }

    private class UnitOfWorkFactoryInstance
        implements UnitOfWorkFactory
    {
        public UnitOfWorkFactoryInstance()
        {
        }

        public UnitOfWork newUnitOfWork()
        {
            return newUnitOfWork( Usecase.DEFAULT );
        }

        public UnitOfWork newUnitOfWork( long currentTime )
        {
            return newUnitOfWork( Usecase.DEFAULT, currentTime );
        }

        public UnitOfWork newUnitOfWork( Usecase usecase )
        {
            return newUnitOfWork( usecase, System.currentTimeMillis() );
        }

        @Override
        public UnitOfWork newUnitOfWork( Usecase usecase, long currentTime )
        {
            return new ModuleUnitOfWork( ModuleInstance.this, new UnitOfWorkInstance( usecase, currentTime ) );
        }

        public UnitOfWork currentUnitOfWork()
        {
            Stack<UnitOfWorkInstance> stack = UnitOfWorkInstance.getCurrent();
            if( stack.size() == 0 )
            {
                return null;
            }
            return new ModuleUnitOfWork( ModuleInstance.this, stack.peek() );
        }

        public UnitOfWork getUnitOfWork( EntityComposite entity )
        {
            EntityInstance instance = EntityInstance.getEntityInstance( entity );
            return instance.unitOfWork();
        }
    }

    private class ServiceFinderInstance
        implements ServiceFinder
    {
        Map<Type, ServiceReference> service = new ConcurrentHashMap<Type, ServiceReference>();
        Map<Type, Iterable<ServiceReference>> services = new ConcurrentHashMap<Type, Iterable<ServiceReference>>();

        public <T> ServiceReference<T> findService( Type serviceType )
        {
            ServiceReference serviceReference = service.get( serviceType );
            if( serviceReference == null )
            {
                ServiceReferenceFinder<T> finder = new ServiceReferenceFinder<T>();
                finder.type = serviceType;

                visitModules( finder );
                serviceReference = finder.service;
                if( serviceReference != null )
                {
                    service.put( serviceType, serviceReference );
                }
            }

            return serviceReference;
        }

        public <T> Iterable<ServiceReference<T>> findServices( Type serviceType )
        {
            Iterable iterable = services.get( serviceType );
            if( iterable == null )
            {
                ServiceReferencesFinder<T> finder = new ServiceReferencesFinder<T>();
                finder.type = serviceType;

                visitModules( finder );
                iterable = finder.services;
                services.put( serviceType, iterable );
            }

            return iterable;
        }
    }

    class ServiceReferenceFinder<T>
        implements ModuleVisitor<RuntimeException>
    {
        public Type type;
        public ServiceReference<T> service;

        public boolean visitModule( ModuleInstance moduleInstance, ModuleModel moduleModel, Visibility visibility )
        {
            service = moduleInstance.getServiceFor( type, visibility );

            return service == null;
        }
    }

    class ServiceReferencesFinder<T>
        implements ModuleVisitor<RuntimeException>
    {
        public Type type;
        public List<ServiceReference<T>> services = new ArrayList<ServiceReference<T>>();

        public boolean visitModule( ModuleInstance moduleInstance, ModuleModel moduleModel, Visibility visibility )
        {
            moduleInstance.getServicesFor( type, visibility, services );

            return true;
        }
    }
}
