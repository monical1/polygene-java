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
import org.apache.zest.api.structure.ApplicationDescriptor;
import org.apache.zest.library.appbrowser.Formatter;

public class ApplicationModelFormatter extends AbstractJsonFormatter<ApplicationDescriptor, Void>
{

    public ApplicationModelFormatter( JSONWriter writer )
    {
        super( writer );
    }

    @Override
    public void enter( ApplicationDescriptor visited )
        throws JSONException
    {
        object();
        field( "name", visited.name() );
        array("layers");
    }

    @Override
    public void leave( ApplicationDescriptor visited )
        throws JSONException
    {
        endArray();
        endObject();
    }

    @Override
    public void visit( Void visited )
        throws JSONException
    {
    }
}
