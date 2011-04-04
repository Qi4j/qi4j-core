package org.qi4j.api.util;

import org.qi4j.api.specification.Specification;
import org.qi4j.api.specification.Specifications;

import static org.qi4j.api.specification.Specifications.in;
import static org.qi4j.api.specification.Specifications.not;
import static org.qi4j.api.util.Iterables.filter;
import static org.qi4j.api.util.Iterables.first;
import static org.qi4j.api.util.Iterables.map;

/**
 * Utility functions. See FunctionsTest for usages.
 */
public class Functions
{
    public static Function<Number, Long> longSum()
    {
        return new Function<Number, Long>()
        {
            long sum;

            @Override
            public Long map( Number number )
            {
                sum += number.longValue();
                return sum;
            }
        };
    }

    public static Function<Number, Integer> intSum()
    {
        return new Function<Number, Integer>()
        {
            int sum;

            @Override
            public Integer map( Number number )
            {
                sum += number.intValue();
                return sum;
            }
        };
    }

    public static <T> Function<T, Integer> count( final Specification<T> specification)
    {
        return new Function<T, Integer>()
        {
            int count;

            @Override
            public Integer map( T item )
            {
                if (specification.satisfiedBy( item ))
                    count++;

                return count;
            }
        };
    }

    public static <T> Function<T, Integer> indexOf( final Specification<T> specification)
    {
        return new Function<T, Integer>()
        {
            int index = -1;
            int current = 0;

            @Override
            public Integer map( T item )
            {
                if (index == -1 && specification.satisfiedBy( item ))
                    index = current;

                current++;

                return index;
            }
        };
    }

    public static <T> int indexOf(T item, Iterable<T> iterable)
    {
        return first( filter( not( in( -1 ) ), map( indexOf( in( item ) ), iterable ) ) );
    }
}
