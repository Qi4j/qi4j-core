/*  Copyright 2007 Niclas Hedhman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.runtime.composite;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.qi4j.composite.Composite;
import org.qi4j.entity.Identity;
import org.qi4j.runtime.structure.ModuleContext;
import org.qi4j.spi.composite.CompositeState;

public abstract class AbstractCompositeInstance
    implements InvocationHandler, CompositeState
{
    protected static final Method METHOD_GETIDENTITY;

    static
    {
        try
        {
            METHOD_GETIDENTITY = Identity.class.getMethod( "getIdentity" );
        }
        catch( NoSuchMethodException e )
        {
            throw new InternalError( Identity.class + " is corrupt." );
        }
    }

    public static CompositeInstance getCompositeInstance( Object aProxy )
    {
        return (CompositeInstance) Proxy.getInvocationHandler( aProxy );
    }

    protected CompositeContext context;
    protected ModuleContext moduleContext;

    public AbstractCompositeInstance( CompositeContext aContext, ModuleContext moduleContext )
    {
        context = aContext;
        this.moduleContext = moduleContext;
    }

    public CompositeContext getContext()
    {
        return context;
    }

    protected Object invokeObject( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        if( method.getName().equals( "hashCode" ) )
        {
            if( context.getCompositeModel().getCompositeClass().isAssignableFrom( Identity.class ) )
            {
                String id = ( (Identity) proxy ).getIdentity();
                if( id != null )
                {
                    return id.hashCode();
                }
                else
                {
                    return 0;
                }
            }
            else
            {
                return 0; // TODO ?
            }
        }
        if( method.getName().equals( "equals" ) )
        {
            if( args[ 0 ] == null )
            {
                return false;
            }
            if( context.getCompositeModel().getCompositeClass().isAssignableFrom( Identity.class ) )
            {
                String id = ( (Identity) proxy ).getIdentity();
                Identity other = ( (Identity) args[ 0 ] );
                return id != null && id.equals( other.getIdentity() );
            }
            else
            {
                return getCompositeInstance( (Composite) proxy ) == this;
            }
        }
        if( method.getName().equals( "toString" ) )
        {
            if( context.getCompositeModel().getCompositeClass().isAssignableFrom( Identity.class ) )
            {
                String id = (String) invoke( proxy, METHOD_GETIDENTITY, null );
                return id != null ? id : "";
            }
            else
            {
                return "";
            }
        }

        return null;
    }
}