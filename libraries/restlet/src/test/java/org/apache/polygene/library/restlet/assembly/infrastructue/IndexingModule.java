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
 *
 *
 */

package org.apache.polygene.library.restlet.assembly.infrastructue;

import org.apache.polygene.api.common.Visibility;
import org.apache.polygene.bootstrap.LayerAssembly;
import org.apache.polygene.bootstrap.ModuleAssembly;
import org.apache.polygene.bootstrap.layered.ModuleAssembler;
import org.apache.polygene.index.rdf.assembly.RdfNativeSesameStoreAssembler;
import org.apache.polygene.library.rdf.repository.NativeConfiguration;

public class IndexingModule
    implements ModuleAssembler
{
    public static final String NAME = "Indexing Module";
    private final ModuleAssembly configModule;

    public IndexingModule( ModuleAssembly configModule )
    {
        this.configModule = configModule;
    }

    @Override
    public ModuleAssembly assemble( LayerAssembly layer, ModuleAssembly module )
    {
//        new FileConfigurationAssembler().assemble( module );
//        new ESFilesystemIndexQueryAssembler()
//            .visibleIn( Visibility.application )
//            .withConfig( configModule, Visibility.application )
//            .assemble( module );

        configModule.entities( NativeConfiguration.class ).visibleIn( Visibility.application );
        new RdfNativeSesameStoreAssembler(Visibility.application, Visibility.module).assemble( module );
        return module;
    }
}
