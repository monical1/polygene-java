/*
 * Copyright 2011 Paul Merlin.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entitystore.mongodb;

import com.mongodb.DB;
import com.mongodb.Mongo;
import org.qi4j.api.concern.Concerns;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.service.ServiceActivation;
import org.qi4j.api.service.ServiceComposite;
import org.qi4j.library.locking.LockingAbstractComposite;
import org.qi4j.spi.entitystore.ConcurrentModificationCheckConcern;
import org.qi4j.spi.entitystore.EntityStateVersions;
import org.qi4j.spi.entitystore.EntityStore;
import org.qi4j.spi.entitystore.StateChangeNotificationConcern;
import org.qi4j.spi.entitystore.helpers.MapEntityStoreMixin;

@Concerns( { StateChangeNotificationConcern.class, ConcurrentModificationCheckConcern.class } )
@Mixins( { MapEntityStoreMixin.class, MongoMapEntityStoreMixin.class } )
public interface MongoMapEntityStoreService
        extends EntityStore,
                EntityStateVersions,
                ServiceComposite,
                ServiceActivation,
                LockingAbstractComposite,
                Configuration
{

    Mongo mongoInstanceUsed();

    DB dbInstanceUsed();

    String collectionUsed();

}
