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

package org.apache.polygene.library.logging.trace;

import java.lang.reflect.InvocationHandler;
import org.apache.polygene.api.common.AppliesTo;
import org.apache.polygene.api.composite.Composite;
import org.apache.polygene.api.injection.scope.Invocation;
import org.apache.polygene.api.injection.scope.This;

/**
 * The TraceConcern will call the traceEntry(), traceExit() and traceException() methods in the
 * Tracer instance associated with the CompositeType that the TraceConcern is part of.
 * <p>
 * The Trace paradigm is all about tracking the entry and exit (both normal and exceptional ones)
 * of methods. The TraceConcern will be added to all methods that declares the <code>@Trace</code>
 * annotation, with an optional <i>level</i> argument. The <i>level</i> is the threshold of the
 * Tracer instance required to trace the method, i.e. the <i>priority</i> of the Tracer instance
 * must be equal or higher than the <i>level</i> set in the <code>@Trace</code> annotation of the
 * method.
 * </p>
 * <p>
 * If the <i>priority</i> of the <code>Tracer</code> is set to OFF (Integer.MIN_VALUE) then no
 * tracing will happen.
 * </p>
 */
@AppliesTo( Trace.class )
public final class TraceConcern extends AbstractTraceConcern
    implements InvocationHandler
{
    @Invocation private Trace trace;

    public TraceConcern( @This Composite thisComposite )
    {
        super( thisComposite );
    }

    @Override
    protected boolean doTrace()
    {
        return traceService.traceLevel() <= trace.level();
    }
}
