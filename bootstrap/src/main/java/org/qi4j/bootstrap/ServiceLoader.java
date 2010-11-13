package org.qi4j.bootstrap;

import java.io.IOException;

public interface ServiceLoader
{
    <T> Iterable<T> providers( Class<T> neededType )
        throws IOException;

    <T> T firstProvider( Class<T> neededType )
            throws IOException;
}
