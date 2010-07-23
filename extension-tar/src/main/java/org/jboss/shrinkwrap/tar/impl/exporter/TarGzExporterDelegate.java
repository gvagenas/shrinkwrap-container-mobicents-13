/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shrinkwrap.tar.impl.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.javatar.TarEntry;
import org.jboss.javatar.TarGzOutputStream;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.impl.base.exporter.StreamExporterDelegateBase;

/**
 * Implementation of an exporter for the TAR format, further encoded as GZIP.  
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class TarGzExporterDelegate extends StreamExporterDelegateBase<TarGzOutputStream>
{
   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(TarGzExporterDelegate.class.getName());

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new exporter delegate for exporting archives as TAR/GZ
    */
   public TarGzExporterDelegate(final Archive<?> archive)
   {
      super(archive);
   }

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.exporter.StreamExporterDelegateBase#closeEntry(java.io.OutputStream)
    */
   @Override
   protected final void closeEntry(final TarGzOutputStream outputStream) throws IOException
   {
      // Close the entry
      outputStream.closeEntry();
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.exporter.StreamExporterDelegateBase#createOutputStream(java.io.OutputStream)
    */
   @Override
   protected final TarGzOutputStream createOutputStream(final OutputStream out) throws IOException
   {
      // Create and return
      return new TarGzOutputStream(out);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.exporter.StreamExporterDelegateBase#putNextExtry(java.io.OutputStream, java.lang.String)
    */
   @Override
   protected final void putNextExtry(final TarGzOutputStream outputStream, final String context) throws IOException
   {
      // Put
      final TarEntry entry = new TarEntry(context);
      outputStream.putNextEntry(entry);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.exporter.StreamExporterDelegateBase#getExportTask()
    */
   @Override
   protected Callable<Void> getExportTask(final Callable<Void> wrappedTask)
   {
      assert wrappedTask != null : "Wrapped task must be specified";
      return new Callable<Void>()
      {

         @Override
         public Void call() throws Exception
         {
            try
            {
               // Attempt the wrapped task
               wrappedTask.call();
            }
            catch (final Exception e)
            {

               // Log this and rethrow; otherwise if we go into deadlock we won't ever 
               // be able to get the underlying cause from the Future 
               log.log(Level.WARNING, "Exception encountered during export of archive", e);

               throw e;
            }
            finally
            {

               try
               {
                  outputStream.close();
               }
               catch (final IOException ioe)
               {
                  // Ignore, but warn of danger
                  log.log(Level.WARNING,
                        "[SHRINKWRAP-120] Possible deadlock scenario: Got exception on closing the TAR.GZ out stream: "
                              + ioe.getMessage(), ioe);
               }
            }

            return null;
         }
      };
   }
}
