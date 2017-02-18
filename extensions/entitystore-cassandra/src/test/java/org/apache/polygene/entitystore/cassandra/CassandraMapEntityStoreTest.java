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
package org.apache.polygene.entitystore.cassandra;

import org.apache.polygene.entitystore.cassandra.assembly.CassandraDBEntityStoreAssembler;
import org.apache.polygene.test.entity.CanRemoveAll;
import org.junit.BeforeClass;
import org.apache.polygene.api.common.Visibility;
import org.apache.polygene.bootstrap.AssemblyException;
import org.apache.polygene.bootstrap.ModuleAssembly;
import org.apache.polygene.test.EntityTestAssembler;
import org.apache.polygene.test.entity.AbstractEntityStoreTest;
import org.apache.polygene.valueserialization.orgjson.OrgJsonValueSerializationAssembler;

import static org.apache.polygene.test.util.Assume.assumeConnectivity;

/**
 * Test the CassandraEntityStoreService.
 * <p>Installing Cassandra and starting it should suffice as the test use Cassandra defaults: 127.0.0.1:3000</p>
 */
public class CassandraMapEntityStoreTest
    extends AbstractEntityStoreTest
{


    @BeforeClass
    public static void beforeCassandraEntityStoreTests()
    {
        assumeConnectivity( "localhost", 9042 );
    }

    @Override
    // START SNIPPET: assembly
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        // END SNIPPET: assembly
        super.assemble( module );

        ModuleAssembly config = module.layer().module( "config" );
        new EntityTestAssembler().assemble( config );
        module.services( CassandraEntityStoreService.class ).withTypes( CanRemoveAll.class ).withMixins( EmptyCassandraTableMixin.class );
        new OrgJsonValueSerializationAssembler().assemble( module );

        // START SNIPPET: assembly
        new CassandraDBEntityStoreAssembler().withConfig( config, Visibility.layer ).assemble( module );
        // END SNIPPET: assembly

        CassandraEntityStoreConfiguration cassandraConfig = config.forMixin( CassandraEntityStoreConfiguration.class ).declareDefaults();
        cassandraConfig.keySpace().set( "polygene:test" );
        cassandraConfig.entityTableName().set( "polygene:test:entities" );
        cassandraConfig.replicationFactor().set( 1 );
        // START SNIPPET: assembly
    }
    // END SNIPPET: assembly

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        CanRemoveAll cleaner = serviceFinder.findService( CanRemoveAll.class ).get();
        cleaner.removeAll();
        super.tearDown();
    }
}
