/*
 * Copyright (c) 2007, Rickard Öberg. All Rights Reserved.
 * Copyright (c) 2007, Niclas Hedhman. All Rights Reserved.
 * Copyright (c) 2007, Alin Dreghiciu. All Rights Reserved.
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

package org.qi4j.runtime.composite;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import org.qi4j.api.composite.Composite;
import org.qi4j.api.mixin.Initializable;
import org.qi4j.api.property.StateHolder;
import org.qi4j.runtime.structure.ModuleInstance;
import org.qi4j.spi.composite.AbstractCompositeDescriptor;
import org.qi4j.spi.composite.CompositeInstance;

/**
 * InvocationHandler for proxy objects.
 */
public class TransientInstance
    implements CompositeInstance, MixinsInstance
{
    public static TransientInstance getCompositeInstance( Composite composite )
    {
        return (TransientInstance) Proxy.getInvocationHandler( composite );
    }

    private final Composite proxy;
    protected final Object[] mixins;
    protected StateHolder state;
    protected final AbstractCompositeModel compositeModel;
    private final ModuleInstance moduleInstance;

    public TransientInstance( AbstractCompositeModel compositeModel,
                              ModuleInstance moduleInstance,
                              Object[] mixins,
                              StateHolder state
    )
    {
        this.compositeModel = compositeModel;
        this.moduleInstance = moduleInstance;
        this.mixins = mixins;
        this.state = state;

        proxy = compositeModel.newProxy( this );
    }

    public Object invoke( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        return compositeModel.invoke( this, proxy, method, args, moduleInstance );
    }

    public <T> T proxy()
    {
        return (T) proxy;
    }

    public <T> T newProxy( Class<T> mixinType )
    {
        return compositeModel.newProxy( this, mixinType );
    }

    public Object invokeComposite( Method method, Object[] args )
        throws Throwable
    {
        return compositeModel.invoke( this, proxy, method, args, moduleInstance );
    }

    public AbstractCompositeDescriptor descriptor()
    {
        return (AbstractCompositeDescriptor) compositeModel;
    }

    public <T> T metaInfo( Class<T> infoType )
    {
        return compositeModel.metaInfo( infoType );
    }

    public Class<? extends Composite> type()
    {
        return compositeModel.type();
    }

    public ModuleInstance module()
    {
        return moduleInstance;
    }

    public AbstractCompositeModel compositeModel()
    {
        return compositeModel;
    }

    public StateHolder state()
    {
        return state;
    }

    public void initializeMixins()
    {
        for( Object mixin : mixins )
        {
            if( mixin instanceof Initializable )
            {
                ( (Initializable) mixin ).initialize();
            }
        }
    }

    public Object invoke( Object composite, Object[] params, CompositeMethodInstance methodInstance )
        throws Throwable
    {
        Object mixin = methodInstance.getMixin( mixins );
        return methodInstance.invoke( proxy, params, mixin );
    }

    public Object invokeObject( Object proxy, Object[] args, Method method )
        throws Throwable
    {
        return method.invoke( this, args );
    }

    @Override
    public boolean equals( Object o )
    {
        if( o == null )
        {
            return false;
        }
        if( !Proxy.isProxyClass( o.getClass() ) )
        {
            return false;
        }
        InvocationHandler handler = Proxy.getInvocationHandler( o );
        if( !handler.getClass().equals( TransientInstance.class ) )
        {
            return false;
        }
        TransientInstance other = (TransientInstance) handler;
        if( other.mixins.length != mixins.length )
        {
            return false;
        }

        for( int i = 0; i < mixins.length; i++ )
        {
            if( !CompositeMixin.class.isAssignableFrom( mixins[ i ].getClass() ) )
            {
                if( !mixins[ i ].equals( other.mixins[ i ] ) )
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hashCode = 0;
        for( Object mixin : mixins )
        {
            if( !CompositeMixin.class.isAssignableFrom( mixin.getClass() ) )
            {
                hashCode = hashCode * 31 + mixin.hashCode();
            }
        }
        return hashCode;
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for( Object mixin : mixins )
        {
            try
            {
                Method toStringMethod = mixin.getClass().getMethod( "toString" );
                Class<?> declaringClass = toStringMethod.getDeclaringClass();
                if( !declaringClass.equals( Object.class ) )
                {
                    if( !first )
                    {
                        buffer.append( ", " );
                    }
                    first = false;
                    buffer.append( mixin.toString() );
                }
            }
            catch( NoSuchMethodException e )
            {
                // Can not happen??
                e.printStackTrace();
            }
        }
        if( first )
        {
            return "TransientInstance{" +
                   "mixins=" + ( mixins == null ? null : Arrays.asList( mixins ) ) +
                   ", state=" + state +
                   ", compositeModel=" + compositeModel +
                   ", moduleInstance=" + moduleInstance +
                   '}';
        }
        return buffer.toString();
    }
}