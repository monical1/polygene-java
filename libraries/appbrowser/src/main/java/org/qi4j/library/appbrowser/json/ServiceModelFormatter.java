/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.zest.library.appbrowser.json;

import org.json.JSONException;
import org.json.JSONWriter;
import org.apache.zest.api.service.ServiceDescriptor;

public class ServiceModelFormatter extends AbstractJsonFormatter<ServiceDescriptor, Void>
{
    public ServiceModelFormatter( JSONWriter writer )
    {
        super( writer );
    }

    @Override
    public void enter( ServiceDescriptor visited )
        throws JSONException
    {
        object();
        field( "identity", visited.identity() );
        field( "type", visited.primaryType().getName() );
        field( "visibility", visited.visibility().toString() );
        Class<Object> config = visited.configurationType();
        if( config != null )
        {
            field( "configuration", config.getName() );
        }
        field( "instantiateOnStartup", visited.isInstantiateOnStartup() );
    }

    @Override
    public void leave( ServiceDescriptor visited )
        throws JSONException
    {
        endObject();
    }

    @Override
    public void visit( Void visited )
        throws JSONException
    {

    }
}
