/*
 * Copyright (c) 2008, Rickard Öberg. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.zest.runtime.composite;

import org.apache.zest.api.common.ConstructionException;
import org.apache.zest.api.composite.CompositeInstance;
import org.apache.zest.api.composite.TransientBuilder;
import org.apache.zest.api.composite.TransientDescriptor;
import org.apache.zest.runtime.property.PropertyInstance;
import org.apache.zest.spi.module.ModelModule;
import org.apache.zest.spi.module.ModuleSpi;

/**
 * JAVADOC
 */
public final class TransientBuilderInstance<T>
    implements TransientBuilder<T>
{
    private ModuleSpi module;
    private TransientModel model;

    // lazy initialized in accessor
    private UsesInstance uses = UsesInstance.EMPTY_USES;

    // lazy initialized in accessor
    private CompositeInstance prototypeInstance;

    private TransientStateInstance state;

    public TransientBuilderInstance( ModelModule<TransientDescriptor> modelModule,
                                     TransientStateInstance state,
                                     UsesInstance uses
    )
    {
        this.model = (TransientModel) modelModule.model();
        this.module = modelModule.module();
        this.state = state;
        this.uses = uses;
    }

    @Override
    public TransientBuilder<T> use( Object... usedObjects )
    {
        uses = uses.use( usedObjects );
        return this;
    }

    @Override
    public T prototype()
    {
        // Instantiate given value type
        if( prototypeInstance == null )
        {
            prototypeInstance = model.newInstance( module, uses, state );
        }

        return prototypeInstance.<T>proxy();
    }

    @Override
    public <K> K prototypeFor( Class<K> mixinType )
    {
        // Instantiate given value type
        if( prototypeInstance == null )
        {
            prototypeInstance = model.newInstance( module, uses, state );
        }

        return prototypeInstance.newProxy( mixinType );
    }

    @Override
    public T newInstance()
        throws ConstructionException
    {
        // Set correct info's (immutable) on the state
        model.state().properties()
            .forEach(
                propertyDescriptor ->
                    ( (PropertyInstance<Object>) state.propertyFor( propertyDescriptor.accessor() ) )
                        .setPropertyInfo( propertyDescriptor ) );

        model.checkConstraints( state );

        CompositeInstance compositeInstance =
            model.newInstance( module, uses, state );
        return compositeInstance.<T>proxy();
    }
}
