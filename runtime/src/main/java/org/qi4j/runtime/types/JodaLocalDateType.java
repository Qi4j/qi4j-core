/*
 * Copyright (c) 2009, Rickard Öberg. All Rights Reserved.
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

package org.qi4j.runtime.types;

import java.lang.reflect.Type;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONWriter;
import org.qi4j.api.structure.Module;

import static org.qi4j.api.common.TypeName.*;

/**
 * Date type. Use ISO8601 format (http://www.w3.org/TR/NOTE-datetime). Assumes UTC time.
 */
public final class JodaLocalDateType
    extends AbstractStringType
{
    public JodaLocalDateType()
    {
        super( nameOf( LocalDate.class ) );
    }

    @Override
    public boolean isDate()
    {
        return true;
    }

    public void toJSON( Object value, JSONWriter json )
        throws JSONException
    {
        LocalDate date = (LocalDate) value;
        json.value( date.toString() );
    }

    public Object toJSON( Object value )
        throws JSONException
    {
        LocalDate date = (LocalDate) value;
        return date.toString();
    }

    public Object fromJSON( Object json, Module module )
    {
        return new LocalDate( json );
    }

    @Override
    public String toQueryParameter( Object value )
        throws IllegalArgumentException
    {
        return value == null ? null : value.toString();
    }

    @Override
    public Object fromQueryParameter( String parameter, Module module )
        throws IllegalArgumentException
    {
        return new LocalDate( parameter );
    }

    public static boolean isDate( Type type )
    {
        if( type instanceof Class )
        {
            Class typeClass = (Class) type;
            return ( typeClass.equals( LocalDate.class ) );
        }
        return false;
    }
}