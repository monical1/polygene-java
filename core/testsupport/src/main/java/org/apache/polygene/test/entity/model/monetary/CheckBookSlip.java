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
package org.apache.polygene.test.entity.model.monetary;

import org.apache.polygene.api.injection.scope.This;
import org.apache.polygene.api.mixin.Mixins;
import org.apache.polygene.api.property.Property;

@Mixins( CheckBookSlip.CheckBookSlipMixin.class )
public interface CheckBookSlip
{
    String name();

    Currency amount();

    interface State
    {
        Property<String> name();

        Property<Currency> amount();
    }

    class CheckBookSlipMixin
        implements CheckBookSlip
    {
        @This
        private State state;

        @Override
        public String name()
        {
            return state.name().get();
        }

        @Override
        public Currency amount()
        {
            return state.amount().get();
        }
    }
}
