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

package org.apache.polygene.index.elasticsearch;

import java.io.IOException;
import org.apache.polygene.api.common.Visibility;
import org.apache.polygene.bootstrap.AssemblyException;
import org.apache.polygene.bootstrap.ModuleAssembly;
import org.apache.polygene.index.elasticsearch.assembly.ESClientIndexQueryAssembler;
import org.apache.polygene.library.fileconfig.FileConfigurationAssembler;
import org.apache.polygene.library.fileconfig.FileConfigurationOverride;
import org.apache.polygene.test.EntityTestAssembler;
import org.apache.polygene.test.indexing.AbstractQueryTest;
import org.apache.polygene.test.util.NotYetImplemented;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import static org.apache.polygene.test.util.Assume.assumeNoIbmJdk;

public class ElasticSearchQueryTest extends AbstractQueryTest
{
    @BeforeClass
    public static void beforeClass_IBMJDK()
    {
        assumeNoIbmJdk();
    }

    @ClassRule
    public static final TemporaryFolder ELASTIC_SEARCH_DIR = new TemporaryFolder();

    @ClassRule
    public static final ESEmbeddedRule ELASTIC_SEARCH = new ESEmbeddedRule( ELASTIC_SEARCH_DIR );

    @Rule
    public final TestName testName = new TestName();

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Override
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        super.assemble( module );

        // Config module
        ModuleAssembly config = module.layer().module( "config" );
        new EntityTestAssembler().assemble( config );

        // Index/Query
        new ESClientIndexQueryAssembler( ELASTIC_SEARCH.client() )
            .withConfig( config, Visibility.layer )
            .assemble( module );
        ElasticSearchIndexingConfiguration esConfig = config.forMixin( ElasticSearchIndexingConfiguration.class ).declareDefaults();
        esConfig.index().set( ELASTIC_SEARCH.indexName( ElasticSearchQueryTest.class.getName(),
                                                        testName.getMethodName() ) );
        esConfig.indexNonAggregatedAssociations().set( Boolean.TRUE );

        // FileConfig
        new FileConfigurationAssembler()
            .withOverride( new FileConfigurationOverride().withConventionalRoot( tmpDir.getRoot() ) )
            .assemble( module );
    }

    @NotYetImplemented( reason = "IndexExporter service not implemented" )
    @Test
    @Override
    public void showNetwork()
        throws IOException
    {
        super.showNetwork();
    }

    @NotYetImplemented( reason = "oneOf() Query Expression not implemented" )
    @Test
    @Override
    public void script23()
    {
        super.script23();
    }

    @NotYetImplemented(
        reason = "ElasticSearch perform automatic TimeZone resolution when querying on dates, "
                 + "this test assert that the underlying Index/Query engine do not."
    )
    @Test
    @Override
    public void script42_DateTime()
    {
        super.script42_DateTime();
    }
}
