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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.TypeTokens;
import com.google.common.reflect.TypeToken;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.polygene.api.association.AssociationDescriptor;
import org.apache.polygene.api.common.Optional;
import org.apache.polygene.api.common.QualifiedName;
import org.apache.polygene.api.entity.EntityDescriptor;
import org.apache.polygene.api.entity.EntityReference;
import org.apache.polygene.api.identity.HasIdentity;
import org.apache.polygene.api.identity.Identity;
import org.apache.polygene.api.identity.IdentityGenerator;
import org.apache.polygene.api.identity.StringIdentity;
import org.apache.polygene.api.injection.scope.Service;
import org.apache.polygene.api.injection.scope.Structure;
import org.apache.polygene.api.injection.scope.This;
import org.apache.polygene.api.property.PropertyDescriptor;
import org.apache.polygene.api.service.ServiceActivation;
import org.apache.polygene.api.service.qualifier.Tagged;
import org.apache.polygene.api.structure.Application;
import org.apache.polygene.api.structure.ModuleDescriptor;
import org.apache.polygene.api.unitofwork.NoSuchEntityTypeException;
import org.apache.polygene.api.unitofwork.UnitOfWork;
import org.apache.polygene.api.usecase.Usecase;
import org.apache.polygene.api.value.ValueSerialization;
import org.apache.polygene.spi.entity.EntityState;
import org.apache.polygene.spi.entity.EntityStatus;
import org.apache.polygene.spi.entity.ManyAssociationState;
import org.apache.polygene.spi.entity.NamedAssociationState;
import org.apache.polygene.spi.entitystore.DefaultEntityStoreUnitOfWork;
import org.apache.polygene.spi.entitystore.EntityStore;
import org.apache.polygene.spi.entitystore.EntityStoreSPI;
import org.apache.polygene.spi.entitystore.EntityStoreUnitOfWork;
import org.apache.polygene.spi.entitystore.StateCommitter;
import org.apache.polygene.spi.entitystore.helpers.DefaultEntityState;

import static java.util.stream.StreamSupport.stream;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.APP_VERSION_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.ASSOCIATIONS_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.IDENTITY_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.LASTMODIFIED_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.MANYASSOCIATIONS_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.NAMEDASSOCIATIONS_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.PROPERTIES_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.STORE_VERSION_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.TYPE_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.USECASE_COLUMN;
import static org.apache.polygene.entitystore.cassandra.CassandraCluster.VERSION_COLUMN;

/**
 * MongoDB implementation of MapEntityStore.
 */
public class CassandraEntityStoreMixin
    implements EntityStore, EntityStoreSPI
{

    @This
    private CassandraCluster cluster;

    @Structure
    private Application application;

    @Optional
    @Service
    private CassandraMigration migration;

    @Service
    @Tagged( ValueSerialization.Formats.JSON )
    private ValueSerialization valueSerialization;


    @Optional
    @Service
    private IdentityGenerator idGenerator;

    @Override
    public EntityState newEntityState( EntityStoreUnitOfWork unitOfWork, EntityReference reference, EntityDescriptor entityDescriptor )
    {
        return new DefaultEntityState( unitOfWork.currentTime(), reference, entityDescriptor );
    }

    @Override
    public EntityState entityStateOf( EntityStoreUnitOfWork unitOfWork, ModuleDescriptor module, EntityReference reference )
    {
        return queryFor( cluster.entityRetrieveStatement().bind( reference.toURI() ), module, reference  );
    }

    private EntityState queryFor( BoundStatement statement, ModuleDescriptor module, EntityReference reference )
    {
        ResultSet result = cluster.session().execute( statement );
        Row row = result.one();
        return deserialize( row, module);
    }

    private EntityState deserialize( Row row, ModuleDescriptor module )
    {
        String version = row.getString( VERSION_COLUMN );
        Instant lastModifed = row.getTimestamp( LASTMODIFIED_COLUMN ).toInstant();
        EntityStatus[] status = new EntityStatus[ 1 ];
        status[ 0 ] = EntityStatus.LOADED;

        // Check if version is correct
        String currentAppVersion = row.getString( APP_VERSION_COLUMN );
        if( !currentAppVersion.equals( application.version() ) )
        {
            if( migration != null )
            {
                migration.migrate( row, application.version(), cluster.session() );
                // State may have changed
                status[ 0 ] = EntityStatus.UPDATED;
            }
//            else
//            {
            // Do nothing ?? Should we update to newer version? Probably not...
//            }
        }
        String type = row.getString( TYPE_COLUMN );

        EntityDescriptor entityDescriptor = module.entityDescriptor( type );
        if( entityDescriptor == null )
        {
            throw new NoSuchEntityTypeException( type, module.name(), module.typeLookup() );
        }
        Map<String, String> storedProperties = row.getMap( PROPERTIES_COLUMN, String.class, String.class );
        Map<String, String> storedAssociations = row.getMap( ASSOCIATIONS_COLUMN, String.class, String.class );

        TypeToken<String> stringToken = TypeToken.of( String.class );
        TypeToken<List<String>> listOfStringsToken = TypeTokens.listOf( String.class );
        TypeToken<Map<String, String>> mapOfStringsToken = TypeTokens.mapOf( String.class, String.class );
        Map<String, List<String>> storedManyassociation = row.getMap( MANYASSOCIATIONS_COLUMN, stringToken, listOfStringsToken );
        Map<String, Map<String, String>> storedNamedassociation = row.getMap( NAMEDASSOCIATIONS_COLUMN, stringToken, mapOfStringsToken );

        Map<QualifiedName, Object> properties = new HashMap<>();
        entityDescriptor
            .state()
            .properties()
            .forEach(
                propertyDescriptor ->
                {
                    String storedValue;
                    try
                    {
                        storedValue = storedProperties.get( propertyDescriptor.qualifiedName().name() );
                        if( storedValue == null )
                        {
                            properties.put( propertyDescriptor.qualifiedName(), null );
                        }
                        else
                        {
                            Object deserialized = valueSerialization.deserialize( module, propertyDescriptor.valueType(), storedValue );
                            properties.put( propertyDescriptor.qualifiedName(), deserialized );
                        }
                    }
                    catch( RuntimeException e )
                    {
                        // Value not found, or value is corrupt, default it. Is this correct behavior?
                        Object initialValue = propertyDescriptor.resolveInitialValue( module );
                        properties.put( propertyDescriptor.qualifiedName(), initialValue );
                        status[ 0 ] = EntityStatus.UPDATED;
                    }
                } );

        Map<QualifiedName, EntityReference> associations = new HashMap<>();
        entityDescriptor
            .state()
            .associations()
            .forEach(
                associationType ->
                {
                    try
                    {
                        String storedValue = storedAssociations.get( associationType.qualifiedName().name() );
                        EntityReference value = storedValue == null
                                                ? null
                                                : EntityReference.parseEntityReference( storedValue );
                        associations.put( associationType.qualifiedName(), value );
                    }
                    catch( RuntimeException e )
                    {
                        // Association not found, default it to null
                        associations.put( associationType.qualifiedName(), null );
                        status[ 0 ] = EntityStatus.UPDATED;
                    }
                } );

        Map<QualifiedName, List<EntityReference>> manyAssociations = new HashMap<>();
        entityDescriptor
            .state()
            .manyAssociations()
            .forEach(
                manyAssociationType ->
                {
                    List<EntityReference> references = new ArrayList<>();
                    try
                    {
                        List<String> storedValue = storedManyassociation.get( manyAssociationType.qualifiedName().name() );
                        if( storedValue != null )
                        {
                            for( String value : storedValue )
                            {
                                EntityReference ref = value == null
                                                      ? null
                                                      : EntityReference.parseEntityReference( value );
                                references.add( ref );
                            }
                            manyAssociations.put( manyAssociationType.qualifiedName(), references );
                        }
                    }
                    catch( RuntimeException e )
                    {
                        // ManyAssociation not found, default to empty one
                        manyAssociations.put( manyAssociationType.qualifiedName(), references );
                    }
                } );

        Map<QualifiedName, Map<String, EntityReference>> namedAssociations = new HashMap<>();
        entityDescriptor
            .state()
            .namedAssociations()
            .forEach(
                namedAssociationType ->
                {
                    Map<String, EntityReference> references = new LinkedHashMap<>();
                    try
                    {
                        Map<String, String> storedValues = storedNamedassociation.get( namedAssociationType.qualifiedName().name() );
                        if( storedValues != null )
                        {
                            for( Map.Entry<String, String> entry : storedValues.entrySet() )
                            {
                                String name = entry.getKey();
                                String value = entry.getValue();
                                EntityReference ref = value == null
                                                      ? null
                                                      : EntityReference.parseEntityReference( value );
                                references.put( name, ref );
                            }
                            namedAssociations.put( namedAssociationType.qualifiedName(), references );
                        }
                    }
                    catch( RuntimeException e )
                    {
                        // NamedAssociation not found, default to empty one
                        namedAssociations.put( namedAssociationType.qualifiedName(), references );
                    }
                } );

        EntityReference reference = EntityReference.parseEntityReference( storedProperties.get( "identity" ) );
        return new DefaultEntityState( version,
                                       lastModifed,
                                       reference,
                                       status[ 0 ],
                                       entityDescriptor,
                                       properties,
                                       associations,
                                       manyAssociations,
                                       namedAssociations
        );
    }

    @Override
    public String versionOf( EntityStoreUnitOfWork unitOfWork, EntityReference reference )
    {
        ResultSet result = cluster.session().execute( cluster.entityRetrieveStatement().bind( reference.toURI() ) );
        Row row = result.one();
        return row.getString( VERSION_COLUMN );
    }

    @Override
    public StateCommitter applyChanges( EntityStoreUnitOfWork unitOfWork, Iterable<EntityState> state )
    {
        return new StateCommitter()
        {
            @Override
            public void commit()
            {

                stream( state.spliterator(), false )
                    .filter( entity -> entity.status() == EntityStatus.UPDATED || entity.status() == EntityStatus.NEW )
                    .forEach(
                        entityState ->
                        {
                            Map<String, String> props = new HashMap<>();
                            Map<String, String> assocs = new HashMap<>();
                            Map<String, List<String>> many = new HashMap<>();
                            Map<String, Map<String, String>> named = new HashMap<>();
                            serializeProperties( entityState, props );
                            serializeAssociations( entityState, assocs );
                            serializeManyAssociations( entityState, many );
                            serializeNamedAssociations( entityState, named );
                            Identity identity = (Identity) entityState.propertyValueOf( HasIdentity.IDENTITY_STATE_NAME );
                            String ver = entityState.version();
                            String appVersion = application.version();
                            Date lastModified = new Date( entityState.lastModified().toEpochMilli() );
                            String usecase = unitOfWork.usecase().name();
                            BoundStatement statement = cluster.entityUpdateStatement().bind( identity.toString(), ver, appVersion, lastModified, usecase, props, assocs, many, named );
                            ResultSet result = cluster.session().execute( statement );
                            System.out.println( result );
                        } );
            }

            private void serializeProperties( EntityState entityState, Map<String, String> props )
            {
                Stream<? extends PropertyDescriptor> properties = entityState.entityDescriptor().state().properties();
                properties.forEach(
                    descriptor ->
                    {
                        Object value = entityState.propertyValueOf( descriptor.qualifiedName() );
                        String serialized = valueSerialization.serialize( value );
                        props.put( descriptor.qualifiedName().name(), serialized );
                    } );
            }

            private void serializeAssociations( EntityState entityState, Map<String, String> assocs )
            {
                Stream<? extends AssociationDescriptor> associations = entityState.entityDescriptor().state().associations();
                associations.forEach(
                    descriptor ->
                    {
                        EntityReference ref = entityState.associationValueOf( descriptor.qualifiedName() );
                        assocs.put( descriptor.qualifiedName().name(), ref.toString() );
                    } );
            }

            private void serializeManyAssociations( EntityState entityState, Map<String, List<String>> many )
            {
                Stream<? extends AssociationDescriptor> associations = entityState.entityDescriptor().state().associations();
                associations.forEach(
                    descriptor ->
                    {
                        ManyAssociationState references = entityState.manyAssociationValueOf( descriptor.qualifiedName() );
                        List<String> refs = references.stream().map( EntityReference::toString ).collect( Collectors.toList() );
                        many.put( descriptor.qualifiedName().name(), refs );
                    } );
            }

            private void serializeNamedAssociations( EntityState entityState, Map<String, Map<String, String>> named )
            {
                Stream<? extends AssociationDescriptor> associations = entityState.entityDescriptor().state().associations();
                associations.forEach(
                    descriptor ->
                    {
                        NamedAssociationState references = entityState.namedAssociationValueOf( descriptor.qualifiedName() );
                        Map<String, String> refs =
                            references.stream()
                                      .collect(
                                          Collectors.toMap( Map.Entry::getKey,
                                                            entry -> entry.getValue().toString() ) );
                        named.put( descriptor.qualifiedName().name(), refs );
                    } );
            }

            @Override
            public void cancel()
            {
            }
        };
    }

    @Override
    public EntityStoreUnitOfWork newUnitOfWork( ModuleDescriptor module, Usecase usecase, Instant currentTime )
    {
        Identity newIdentity;
        if( idGenerator == null )
        {
            newIdentity = new StringIdentity( UUID.randomUUID().toString() );
        }
        else
        {
            newIdentity= idGenerator.generate( UnitOfWork.class );
        }
        return new DefaultEntityStoreUnitOfWork( module, this, newIdentity, usecase, currentTime );
    }

    @Override
    public Stream<EntityState> entityStates( ModuleDescriptor module )
    {
        ResultSet resultSet = cluster.session().execute( "SELECT "
                                             + IDENTITY_COLUMN + ", "
                                             + VERSION_COLUMN + ", "
                                             + APP_VERSION_COLUMN + ", "
                                             + STORE_VERSION_COLUMN + ", "
                                             + LASTMODIFIED_COLUMN + ", "
                                             + USECASE_COLUMN + ", "
                                             + PROPERTIES_COLUMN + ", "
                                             + ASSOCIATIONS_COLUMN + ", "
                                             + MANYASSOCIATIONS_COLUMN + ", "
                                             + NAMEDASSOCIATIONS_COLUMN
                                             + " FROM " + cluster.tableName() );
        return stream(resultSet.spliterator(), false).map( row -> deserialize( row, module ));
    }

}
