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

package org.qi4j.runtime.structure.qi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.qi4j.runtime.service.ServiceReferenceInstance;
import org.qi4j.runtime.service.qi.ServiceModel;
import org.qi4j.service.Activatable;
import org.qi4j.service.ServiceReference;
import org.qi4j.structure.Visibility;

/**
 * TODO
 */
public class ServicesInstance
    implements Activatable
{
    private ServicesModel servicesModel;
    private List<ServiceReferenceInstance> serviceReferences;
    private Activator activator;
    private Map<String, ServiceReferenceInstance> mapIdentityServiceReference = new HashMap<String, ServiceReferenceInstance>();

    public ServicesInstance( ServicesModel servicesModel, List<ServiceReferenceInstance> serviceReferences )
    {
        this.servicesModel = servicesModel;
        this.serviceReferences = serviceReferences;

        for( ServiceReferenceInstance serviceReference : serviceReferences )
        {
            mapIdentityServiceReference.put( serviceReference.identity(), serviceReference );
        }

        activator = new Activator();
    }

    public void activate() throws Exception
    {
        activator.activate( serviceReferences );
    }

    public void passivate() throws Exception
    {
        activator.passivate();
    }

    public <T> Iterable<ServiceReference<T>> getServiceReferencesFor( Class<T> serviceType, Visibility visibility )
    {
        Iterable<ServiceModel> serviceModels = servicesModel.getServiceModelsFor( serviceType, visibility );

        List<ServiceReference<T>> serviceReferences = new ArrayList<ServiceReference<T>>();
        for( ServiceModel serviceModel : serviceModels )
        {
            serviceReferences.add( mapIdentityServiceReference.get( serviceModel.identity() ) );
        }
        return serviceReferences;
    }

}