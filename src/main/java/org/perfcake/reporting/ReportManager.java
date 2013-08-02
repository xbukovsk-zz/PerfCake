/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting;

import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hornetq.utils.ConcurrentHashSet;
import org.perfcake.RunInfo;
import org.perfcake.reporting.reporters.Reporter;

/**
 * ReportManager that controls the reporting facilities.
 * 
 * @author Martin Večera <marvenec@gmail.com>
 * 
 */
public class ReportManager {

   private static final Logger log = Logger.getLogger(ReportManager.class);

   /**
    * Set of reporters registered for reporting.
    */
   private final Set<Reporter> reporters = new ConcurrentHashSet<>();

   /**
    * Current run info to control the measurement.
    */
   private RunInfo runInfo;

   /**
    * Create a new measurement unit with a unique iteration number.
    * 
    * @return A new measurement unit with a unique iteration number, or null if a measurement is not running or is already finished.
    */
   public MeasurementUnit newMeasurementUnit() {
      if (!runInfo.isRunning()) {
         return null;
      }

      if (log.isTraceEnabled()) {
         log.trace("Creating a new measurement unit.");
      }

      return new MeasurementUnit(runInfo.getNextIteration());
   }

   /**
    * Set {@link org.perfcake.RunInfo} for the current measurement run.
    * 
    * @param runInfo
    *           The RunInfo that contains information about the current measurement.
    */
   public void setRunInfo(final RunInfo runInfo) {
      if (log.isDebugEnabled()) {
         log.debug("A new run info set " + runInfo);
      }

      this.runInfo = runInfo;
      for (final Reporter r : reporters) {
         r.setRunInfo(runInfo);
      }
   }

   /**
    * Report a newly measured {@link MeasurementUnit}. Each Measurement Unit must be reported exactly once.
    * 
    * @param mu
    *           A MeasurementUnit to be reported.
    * @throws ReportingException
    *            If reporting could not be done properly.
    */
   public void report(final MeasurementUnit mu) throws ReportingException {
      if (log.isTraceEnabled()) {
         log.trace("Reporting a new measurement unit " + mu);
      }

      ReportingException e = null;

      if (runInfo.isStarted()) { // cannot use isRunning while we still want the last iteration to be reported
         for (final Reporter r : reporters) {
            try {
               r.report(mu);
            } catch (final ReportingException re) {
               log.warn("Error reporting a measurement unit " + mu, re);
               e = re; // store the latest exception and give chance to other reporters as well
            }
         }
      } else {
         log.info("Skipping the measurement unit (" + mu + ") because the ReportManager has not been started yet.");
      }

      if (e != null) {
         throw e;
      }
   }

   /**
    * Resets reporting to the zero state. It is used after the warm-up period.
    */
   public void reset() {
      if (log.isDebugEnabled()) {
         log.debug("Reseting reporting.");
      }

      runInfo.reset();
      for (final Reporter r : reporters) {
         r.reset();
      }
   }

   /**
    * Registers a new {@link org.perfcake.reporting.reporters.Reporter}.
    * 
    * @param reporter
    *           A reporter to be registered.
    */
   public void registerReporter(final Reporter reporter) {
      if (log.isDebugEnabled()) {
         log.debug("Registering a new reporter " + reporter);
      }

      reporter.setRunInfo(runInfo);
      reporters.add(reporter);
   }

   /**
    * Removes a registered {@link org.perfcake.reporting.reporters.Reporter}.
    * 
    * @param reporter
    *           A reporter to unregistered.
    */
   public void unregisterReporter(final Reporter reporter) {
      if (log.isDebugEnabled()) {
         log.debug("Removing the reporter " + reporter);
      }

      reporters.remove(reporter);
   }

   /**
    * Gets an immutable set of current reporters.
    * 
    * @return An immutable set of currently registered reporters.
    */
   public Set<Reporter> getReporters() {
      return Collections.unmodifiableSet(reporters);
   }

   /**
    * Starts the reporting facility.
    */
   public void start() {
      if (log.isDebugEnabled()) {
         log.debug("Starting reporting and all reporters.");
      }

      runInfo.start(); // runInfo must be started first, otherwise the time monitoring thread in AbstractReporter dies immediately

      for (final Reporter r : reporters) {
         r.start();
      }
   }

   /**
    * Stops the reporting facility.
    */
   public void stop() {
      if (log.isDebugEnabled()) {
         log.debug("Stopping reporting and all reporters.");
      }

      runInfo.stop();

      for (final Reporter r : reporters) {
         r.stop();
      }
   }

}
