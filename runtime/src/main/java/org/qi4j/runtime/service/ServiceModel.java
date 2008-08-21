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
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Set;
import org.qi4j.composite.Composite;
import org.qi4j.runtime.structure.ModelVisitor;
import org.qi4j.service.ServiceDescriptor;
import org.qi4j.service.ServiceInstanceFactory;
import org.qi4j.structure.Module;
import org.qi4j.structure.Visibility;
import org.qi4j.util.MetaInfo;
import org.qi4j.util.ClassUtil;

/**
 * TODO
 */
public final class ServiceModel
    implements ServiceDescriptor
{
    private final Class<? extends Composite> type;
    private final Visibility visibility;
    private final Class<? extends ServiceInstanceFactory> serviceFactory;
    private final String identity;
    private final boolean instantiateOnStartup;
    private final MetaInfo metaInfo;
    private String moduleName;

    public ServiceModel( Class<? extends Composite> compositeType,
                         Visibility visibility,
                         Class<? extends ServiceInstanceFactory> serviceFactory,
                         String identity,
                         boolean instantiateOnStartup, MetaInfo metaInfo, String moduleName )
    {
        type = compositeType;
        this.visibility = visibility;
        this.serviceFactory = serviceFactory;
        this.identity = identity;
        this.instantiateOnStartup = instantiateOnStartup;
        this.metaInfo = metaInfo;
        this.moduleName = moduleName;
    }

    public Class<? extends Composite> type()
    {
        return type;
    }

    public Visibility visibility()
    {
        return visibility;
    }

    public MetaInfo metaInfo()
    {
        return metaInfo;
    }

    public boolean isInstantiateOnStartup()
    {
        return instantiateOnStartup;
    }

    public Class<? extends ServiceInstanceFactory> serviceFactory()
    {
        return serviceFactory;
    }

    public String identity()
    {
        return identity;
    }

    public String moduleName()
    {
        return moduleName;
    }

    public void visitModel( ModelVisitor modelVisitor )
    {
        modelVisitor.visit( this );
    }


    public boolean isServiceFor( Type serviceType, Visibility visibility )
    {
        // Check visibility
        if (visibility != this.visibility)
            return false;

        // Check types
        if (serviceType instanceof Class)
        {
            // Plain class check
            Class serviceClass = ( Class) serviceType;
            return serviceClass.isAssignableFrom( type );
        } else if (serviceType instanceof ParameterizedType )
        {
            // Parameterized type check. This is useful for example Wrapper<Foo> usages
            ParameterizedType paramType = ( ParameterizedType) serviceType;
            Class rawClass = (Class) paramType.getRawType();
            Set<Type> types = ClassUtil.genericInterfacesOf( type );
            for( Type type1 : types )
            {
                if (type1 instanceof ParameterizedType && rawClass.isAssignableFrom( ClassUtil.getRawClass( type1 )))
                {
                    // Check params
                    Type[] actualTypes = paramType.getActualTypeArguments();
                    Type[] actualServiceTypes = ((ParameterizedType)type1).getActualTypeArguments();
                    for( int i = 0; i < actualTypes.length; i++ )
                    {
                        Type actualType = actualTypes[ i ];
                        if (actualType instanceof Class)
                        {
                            Class actualClass = ( Class) actualType;
                            Class actualServiceType = ( Class) actualServiceTypes[i];
                            if (!actualClass.isAssignableFrom( actualServiceType ))
                                return false;
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public ServiceInstance<?> newInstance( Module module )
    {
        ServiceInstanceFactory instanceFactory = module.objectBuilderFactory().newObject( serviceFactory );
        Object instance = instanceFactory.newInstance( this );
        return new ServiceInstance<Object>( instance, instanceFactory, this );
    }

    public Object newProxy( InvocationHandler serviceInvocationHandler )
    {
        if( type.isInterface() )
        {
            return Proxy.newProxyInstance( type.getClassLoader(),
                                           new Class[]{ type },
                                           serviceInvocationHandler );
        }
        else
        {
            Class[] interfaces = type.getInterfaces();
            return Proxy.newProxyInstance( type.getClassLoader(),
                                           interfaces,
                                           serviceInvocationHandler );
        }

    }

    @Override public String toString()
    {
        return type.getName() + ":" + identity;
    }
}
