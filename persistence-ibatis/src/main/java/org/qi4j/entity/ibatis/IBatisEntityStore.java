/*  Copyright 2008 Edward Yakop.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entity.ibatis;

import com.ibatis.sqlmap.client.SqlMapClient;
import static com.ibatis.sqlmap.client.SqlMapClientBuilder.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.qi4j.composite.Composite;
import static org.qi4j.composite.NullArgumentException.*;
import org.qi4j.entity.UnitOfWork;
import org.qi4j.entity.ibatis.dbInitializer.DBInitializer;
import org.qi4j.entity.ibatis.dbInitializer.DBInitializerInfo;
import org.qi4j.entity.ibatis.internal.IBatisEntityState;
import org.qi4j.entity.ibatis.internal.IBatisEntityStateDao;
import static org.qi4j.entity.ibatis.internal.common.Status.*;
import org.qi4j.service.Activatable;
import org.qi4j.spi.composite.CompositeBinding;
import org.qi4j.spi.composite.CompositeModel;
import org.qi4j.spi.composite.CompositeResolution;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.EntityStatus;
import org.qi4j.spi.entity.EntityStore;
import org.qi4j.spi.entity.StateCommitter;
import org.qi4j.spi.entity.StoreException;
import org.qi4j.spi.structure.ServiceDescriptor;

/**
 * TODO: Figure out how does transaction supposed for all EntityStore methods.
 * TODO: identity is a keyword in SQL. We need to have an alias for this identity property for query purposes.
 *
 * @author edward.yakop@gmail.com
 */
final class IBatisEntityStore
    implements EntityStore, Activatable
{
    private final IBatisEntityStoreServiceInfo serviceInfo;
    private final DBInitializerInfo dbInitializerInfo;
    private final IBatisEntityStateDao dao;

    private SqlMapClient client;

    /**
     * Construct a new instance of {@code IBatisEntityStore}.
     *
     * @param aServiceDescriptor The service descriptor. This argument must not be {@code null}.
     * @throws IllegalArgumentException Thrown if the specified {@code aServiceDescriptor} argument is {@code null}.
     * @since 0.1.0
     */
    IBatisEntityStore( ServiceDescriptor aServiceDescriptor )
        throws IllegalArgumentException
    {
        validateNotNull( "aServiceDescriptor", aServiceDescriptor );

        Map<Class, Serializable> serviceInfos = aServiceDescriptor.getServiceInfos();
        serviceInfo = (IBatisEntityStoreServiceInfo) serviceInfos.get( IBatisEntityStoreServiceInfo.class );
        dbInitializerInfo = (DBInitializerInfo) serviceInfos.get( DBInitializerInfo.class );
        dao = new EntityStateDao();

        client = null;
    }

    /**
     * Returns raw data given the composite class.
     *
     * @param anIdentity        The identity. This argument must not be {@code null}.
     * @param aCompositeBinding The composite class. This argument must not be {@code null}.
     * @return The raw data given input.
     * @throws StoreException Thrown if retrieval failed.
     * @since 0.1.0
     */
    private Map getRawData( String anIdentity, CompositeBinding aCompositeBinding )
        throws StoreException
    {
        validateNotNull( "anIdentity", anIdentity );
        validateNotNull( "aCompositeBinding", aCompositeBinding );
        CompositeResolution compositeResolution = aCompositeBinding.getCompositeResolution();
        CompositeModel compositeModel = compositeResolution.getCompositeModel();
        Class<? extends Composite> compositeClass = compositeModel.getCompositeType();
        String statementId = compositeClass.getName() + ".getById";

        // TODO: Transaction?
        try
        {
            return (Map) client.queryForObject( statementId, anIdentity );
        }
        catch( SQLException e )
        {
            throw new StoreException( e );
        }
    }

    /**
     * Throws {@link StoreException} if this service is not active.
     *
     * @throws StoreException Thrown if this service instance is not active.
     * @since 0.1.0
     */
    private void throwIfNotActive()
        throws StoreException
    {
        if( client == null )
        {
            String message = "Possibly bug in the qi4j where the store is not activate but its service is invoked.";
            throw new StoreException( message );
        }
    }

    /**
     * Construct a new entity instance.
     *
     * @param anIdentity        The new entity identity. This argument must not be {@code null}.
     * @param aCompositeBinding The composite binding. This argument must not be {@code null}.
     * @throws IllegalArgumentException Thrown if one or some or all arguments are {@code null}.
     * @throws StoreException           Thrown if creational failed.
     * @since 0.1.0
     */
    public final EntityState newEntityState(
        String anIdentity, CompositeBinding aCompositeBinding )
        throws IllegalArgumentException, StoreException
    {
        validateNotNull( "anIdentity", anIdentity );
        validateNotNull( "aCompositeBinding", aCompositeBinding );

        throwIfNotActive();

        Map<String, Object> fieldValues = new HashMap<String, Object>();

        return new IBatisEntityState( anIdentity, aCompositeBinding, fieldValues, EntityStatus.NEW, statusNew, null, dao );
    }

    /**
     * Returns existing entity instance. Returns {@code null} if not found.
     *
     * @param aUnit             The unit of work. This argument must not be {@code null}.
     * @param anIdentity        The identity. This argument must not be {@code null}.
     * @param aCompositeBinding The composite binding. This argument must not be {@code null}.
     * @return The entity instance with id as {@code anIdentity}.
     * @throws IllegalArgumentException Thrown if one or some or all arguments are {@code null}.
     * @throws StoreException           Thrown if retrieval fail.
     * @since 0.1.0
     */
    @SuppressWarnings( "unchecked" )
    public final IBatisEntityState getEntityState(
        UnitOfWork aUnit, String anIdentity, CompositeBinding aCompositeBinding )
        throws IllegalArgumentException, StoreException
    {
        validateNotNull( "aUnitOfWork", aUnit );
        validateNotNull( "anIdentity", anIdentity );
        validateNotNull( "aCompositeBinding", aCompositeBinding );

        throwIfNotActive();

        Map rawData = getRawData( anIdentity, aCompositeBinding );
        if( rawData == null )
        {
            return null;
        }

        rawData.put( "identity", anIdentity );
        return new IBatisEntityState( anIdentity, aCompositeBinding, rawData, EntityStatus.LOADED, statusLoadFromDb, aUnit, dao );
    }

    /**
     * Complete or persists the list of entity state.
     *
     * @param unitOfWork The unit of work. This argument must not be {@code null}.
     * @param states     The states to complete. This argument must not be {@code null}.
     * @throws StoreException Thrown if the complete failed.
     * @since 0.1.0
     */
    public final StateCommitter prepare( UnitOfWork unitOfWork, Iterable<EntityState> states )
        throws StoreException
    {
        throwIfNotActive();

        for( EntityState state : states )
        {
            IBatisEntityState ibatisState = (IBatisEntityState) state;
            ibatisState.persist();
        }

        // TODO Implement this properly
        return new StateCommitter()
        {
            public void commit()
            {
            }

            public void cancel()
            {
            }
        };
    }

    /**
     * Activate this service.
     *
     * @throws IOException  If reading sql map configuration failed.
     * @throws SQLException Thrown if database initialization failed.
     * @since 0.1.0
     */
    public final void activate()
        throws IOException, SQLException
    {
        // Initialize database if required.
        if( dbInitializerInfo != null )
        {
            DBInitializer dbInitializer = new DBInitializer( dbInitializerInfo );
            dbInitializer.initialize();
        }

        // Initialize client
        String configURL = serviceInfo.getSQLMapConfigURL();
        InputStream configStream = new URL( configURL ).openStream();
        InputStreamReader streamReader = new InputStreamReader( configStream );
        Reader bufferedReader = new BufferedReader( streamReader );

        Properties properties = serviceInfo.getConfigProperties();
        if( properties == null )
        {
            client = buildSqlMapClient( bufferedReader );
        }
        else
        {
            client = buildSqlMapClient( bufferedReader, properties );
        }
    }

    /**
     * Passivate this service.
     *
     * @throws Exception Thrown if there is any passivation problem.
     * @since 0.1.0
     */
    public final void passivate()
        throws Exception
    {
        // clean up client
        client = null;
    }

    private class EntityStateDao
        implements IBatisEntityStateDao
    {
        public boolean deleteComposite( String aCompositeIdentity, CompositeBinding aCompositeBinding )
        {
            // TODO
            return false;
        }
    }
}
