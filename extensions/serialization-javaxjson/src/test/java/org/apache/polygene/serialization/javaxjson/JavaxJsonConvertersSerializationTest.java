/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.polygene.serialization.javaxjson;

import java.io.StringReader;
import javax.json.JsonObject;
import org.apache.polygene.api.injection.scope.Service;
import org.apache.polygene.bootstrap.ModuleAssembly;
import org.apache.polygene.serialization.javaxjson.assembly.JavaxJsonSerializationAssembler;
import org.apache.polygene.test.serialization.AbstractConvertersSerializationTest;

public class JavaxJsonConvertersSerializationTest extends AbstractConvertersSerializationTest
{
    @Override
    public void assemble( ModuleAssembly module )
    {
        new JavaxJsonSerializationAssembler().assemble( module );
        super.assemble( module );
    }

    @Service
    private JavaxJsonFactories jsonFactories;

    @Override
    protected String getStringFromValueState( String state, String key ) throws Exception
    {
        JsonObject jsonObject = jsonFactories.readerFactory().createReader( new StringReader( state ) ).readObject();
        return jsonObject.getString( key );
    }
}
