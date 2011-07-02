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

package org.qi4j.runtime.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.service.ServiceImporterException;
import org.qi4j.api.service.ServiceReference;
import org.qi4j.api.service.ServiceUnavailableException;
import org.qi4j.api.structure.Module;
import org.qi4j.runtime.structure.ModuleInstance;
import org.qi4j.spi.service.ServiceDescriptor;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ServiceReference. This manages the actual instance of the service
 * and implements the invocation of the Activatable interface on the service.
 * <p/>
 * Whenever the service is requested a proxy is returned which points to this class. This means
 * that the instance can be passivated even though a client is holding on to a service proxy.
 */
public final class ServiceReferenceInstance<T>
    implements ServiceReference<T>, Activatable
{
    private volatile ServiceInstance instance;
    private final T serviceProxy;
    private final ModuleInstance module;
    private final ServiceModel serviceModel;
    private final Activator activator = new Activator();

    public ServiceReferenceInstance( ServiceModel serviceModel, ModuleInstance module )
    {
        this.module = module;
        this.serviceModel = serviceModel;

        serviceProxy = newProxy();
    }

    public String identity()
    {
        return serviceModel.identity();
    }

    public <T> T metaInfo( Class<T> infoType )
    {
        return serviceModel.metaInfo( infoType );
    }

    public synchronized T get()
    {
        return serviceProxy;
    }

    public boolean isActive()
    {
        return instance != null;
    }

    public boolean isAvailable()
    {
        return getInstance().isAvailable();
    }

    public Module module()
    {
        return module;
    }

    public void activate()
        throws Exception
    {
        if( serviceModel.isInstantiateOnStartup() )
        {
            getInstance();
        }
    }

    public void passivate()
        throws Exception
    {
        synchronized( this )
        {
            if( instance != null )
            {
                LoggerFactory.getLogger( getClass() ).debug( "Passivating service " + serviceModel.identity() );
                activator.passivate();
                instance = null;
            }
        }
    }

    private ServiceInstance getInstance()
        throws ServiceImporterException
    {
        // DCL that works with Java 1.5 volatile semantics
        if( instance == null )
        {
            synchronized( this )
            {
                if( instance == null )
                {
                    LoggerFactory.getLogger( getClass() ).debug( "Activating service " + serviceModel.identity() );
                    ServiceInstance newInstance = serviceModel.newInstance( module );
                    try
                    {
                        activator.activate( newInstance );
                    }
                    catch( Exception e )
                    {
                        instance = null;
                        throw new ServiceUnavailableException( "Could not activate service " + serviceModel.identity(), e );
                    }
                    instance = newInstance;
                }
            }
        }

        return instance;
    }

    @Override
    public String toString()
    {
        return serviceModel.identity() + ", active=" + isActive() + ", module='" + serviceModel.moduleName() + "'";
    }

    public T newProxy()
    {
        return (T) serviceModel.newProxy( new ServiceReferenceInstance.ServiceInvocationHandler() );
    }

    public ServiceDescriptor serviceDescriptor()
    {
        return serviceModel;
    }

    public final class ServiceInvocationHandler
        implements InvocationHandler
    {
        public Object invoke( Object object, Method method, Object[] objects )
            throws Throwable
        {
            if( method.getDeclaringClass().equals( Object.class ) )
            {
                if( method.getName().equals( "toString" ) )
                {
                    return serviceModel.toString();
                }
                else if( method.getName().equals( "equals" ) )
                {
                    Object obj = objects[ 0 ];
                    return obj == object;
                }
                else if( method.getName().equals( "hashCode" ) )
                {
                    return serviceModel.toString().hashCode();
                }
            }

            ServiceInstance instance = getInstance();

/*
            if (!instance.isAvailable())
            {
                throw new ServiceUnavailableException("Service is currently not available");
            }

*/
            return instance.invoke( object, method, objects );
        }

        @Override
        public String toString()
        {
            return serviceModel.toString();
        }

        public Module module()
        {
            return module;
        }
    }
}
