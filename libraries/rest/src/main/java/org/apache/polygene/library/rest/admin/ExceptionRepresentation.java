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

package org.apache.polygene.library.rest.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.restlet.representation.StringRepresentation;

public class ExceptionRepresentation
    extends StringRepresentation
{
    public ExceptionRepresentation( Throwable e )
    {
        super( getStackTraceAsHtml( e ) );
    }

    private static String getStackTraceAsHtml( Throwable e )
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream( baos );
        e.printStackTrace( out );
        out.close();
        try
        {
            baos.close();
        }
        catch( IOException e1 )
        {
            // Can not happen.
        }
        String text = baos.toString();
        return "<code>" + text + "</code>";
    }
}
