/*
 * Copyright 2009 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qi4j.runtime.types;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.entity.Queryable;
import org.qi4j.api.property.GenericPropertyInfo;
import org.qi4j.api.util.Classes;
import org.qi4j.spi.property.PropertyType;
import org.qi4j.spi.property.ValueType;

import static org.qi4j.api.common.TypeName.*;

public class ValueTypeFactory
{
    private static final ValueTypeFactory instance = new ValueTypeFactory();

    public static ValueTypeFactory instance()
    {
        return instance;
    }

    public ValueType newValueType( Type type, Class declaringClass, Class compositeType )
    {
        return newValueType( null, type, declaringClass, compositeType );
    }

    private ValueType newValueType( Map<Type, ValueType> typeMap, Type type, Class declaringClass, Class compositeType )
    {
        ValueType valueType = null;
        if( CollectionType.isCollection( type ) )
        {
            if( type instanceof ParameterizedType )
            {
                ParameterizedType pt = (ParameterizedType) type;
                Type collectionType = pt.getActualTypeArguments()[ 0 ];
                if( collectionType instanceof TypeVariable )
                {
                    TypeVariable collectionTypeVariable = (TypeVariable) collectionType;
                    collectionType = Classes.resolveTypeVariable( collectionTypeVariable, declaringClass, compositeType );
                }
                ValueType collectedType = newValueType( typeMap, collectionType, declaringClass, compositeType );
                valueType = new CollectionType( nameOf( type ), collectedType );
            }
            else
            {
                valueType = new CollectionType( nameOf( type ), newValueType( typeMap, Object.class, declaringClass, compositeType ) );
            }
        }
        else if( MapType.isMap( type ) )
        {
            if( type instanceof ParameterizedType )
            {
                ParameterizedType pt = (ParameterizedType) type;
                Type keyType = pt.getActualTypeArguments()[ 0 ];
                if( keyType instanceof TypeVariable )
                {
                    TypeVariable keyTypeVariable = (TypeVariable) keyType;
                    keyType = Classes.resolveTypeVariable( keyTypeVariable, declaringClass, compositeType );
                }
                ValueType keyedType = newValueType( typeMap, keyType, declaringClass, compositeType );

                Type valType = pt.getActualTypeArguments()[ 1 ];
                if( valType instanceof TypeVariable )
                {
                    TypeVariable valueTypeVariable = (TypeVariable) valType;
                    valType = Classes.resolveTypeVariable( valueTypeVariable, declaringClass, compositeType );
                }
                ValueType valuedType = newValueType( typeMap, valType, declaringClass, compositeType );

                valueType = new MapType( nameOf( type ), keyedType, valuedType );
            }
            else
            {
                valueType = new MapType( nameOf( type ), newValueType( typeMap, Object.class, declaringClass, compositeType ), newValueType( typeMap, Object.class, declaringClass, compositeType ) );
            }
        }
        else if( ValueCompositeType.isValueComposite( type ) )
        {
            if( typeMap != null )
            {
                valueType = typeMap.get( type );
            }

            if( valueType == null )
            {
                Class valueTypeClass = Classes.getRawClass( type );

                List<PropertyType> types = new ArrayList<PropertyType>();
                valueType = new ValueCompositeType( nameOf( valueTypeClass ), types );
                if( typeMap == null )
                {
                    typeMap = new HashMap<Type, ValueType>();
                }
                typeMap.put( type, valueType );

                addProperties( typeMap, valueTypeClass, compositeType, types );
                // The Properties of Private Mixins are missed here, since we don't leverage StateModel. QI-328
                Collections.sort( types ); // Sort by property name
            }
        }
        else if( EnumType.isEnum( type ) )
        {
            valueType = new EnumType( nameOf( type ) );
        }
        else if( StringType.isString( type ) )
        {
            valueType = new StringType();
        }
        else if( NumberType.isNumber( type ) )
        {
            valueType = new NumberType( nameOf( type ) );
        }
        else if( BooleanType.isBoolean( type ) )
        {
            valueType = new BooleanType();
        }
        else if( DateType.isDate( type ) )
        {
            valueType = new DateType();
        }
        else if( JodaDateTimeType.isDate( type ) )
        {
            valueType = new JodaDateTimeType();
        }
        else if( JodaLocalDateType.isDate( type ) )
        {
            valueType = new JodaLocalDateType();
        }
        else if( EntityReferenceType.isEntityReference( type ) )
        {
            valueType = new EntityReferenceType( nameOf( type ) );
        }
        else
        {
            // TODO: shouldn't we check that the type is a Serializable?
            valueType = new SerializableType( nameOf( type ) );
        }

        return valueType;
    }

    private void addProperties( Map<Type, ValueType> typeMap,
                                Class valueTypeClass,
                                Class compositeType,
                                List<PropertyType> types
    )
    {
        for( Method method : valueTypeClass.getDeclaredMethods() )
        {
            Type propType = GenericPropertyInfo.getPropertyType( method );
            if( propType != null )
            {
                Queryable queryableAnnotation = method.getAnnotation( Queryable.class );
                boolean queryable = queryableAnnotation == null || queryableAnnotation.value();
                ValueType propValueType = newValueType( typeMap, propType, valueTypeClass, compositeType );
                PropertyTypeImpl propertyType = new PropertyTypeImpl( QualifiedName.fromMethod( method ), propValueType, queryable, PropertyTypeImpl.PropertyTypeEnum.IMMUTABLE );
                types.add( propertyType );
            }
        }

        // Add methods from subinterface
        for( Type subType : valueTypeClass.getGenericInterfaces() )
        {
            // Handles generic type variables
            Class subClass;
            if( subType instanceof ParameterizedType )
            {
                subClass = (Class) ( (ParameterizedType) subType ).getRawType();
            }
            else
            {
                subClass = (Class) subType;
            }

            addProperties( typeMap, subClass, valueTypeClass, types );
        }
    }
}
