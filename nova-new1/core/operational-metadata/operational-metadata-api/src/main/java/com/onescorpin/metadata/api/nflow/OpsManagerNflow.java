package com.onescorpin.metadata.api.nflow;

/*-
 * #%L
 * onescorpin-operational-metadata-api
 * %%
 * Copyright (C) 2017 Onescorpin
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.Serializable;

/**
 * Represents a nflow in the operational store.
 */
public interface OpsManagerNflow extends  Serializable{

    /**
     * @return the unique ID of this Nflow
     */
    ID getId();

    /**
     * @return the name of this Nflow
     */
    String getName();

    /**
     * {@link NflowType#NFLOW} is the default type and represents the majority of nflows in the system {@link NflowType#CHECK} represents a Data Confidence check nflow.  {@link NflowType#CHECK} nflows are
     * new nflow flows that have a pointer back to a specific nflow for which to do a Data Confidence check on.
     *
     * @return the type of nflow
     */
    NflowType getNflowType();

    /**
     * The type of nflow
     * NFLOW is the default type and represents the majority of nflows in the system
     * CHECK represents a Data Confidence check nflow.  CHECK nflows are new nflow flows that have a pointer back to a specific nflow for which to do a Data Confidence check on.
     */
    enum NflowType {
        NFLOW, CHECK
    }


    /**
     * The ID for the Nflow
     */
    interface ID extends Serializable, Nflow.ID {

    }

    /**
     *
     * @return true if streaming nflow, false if not
     */
    boolean isStream();


    /**
     * For Batch Nflows that may start many flowfiles/jobs at once in a short amount of time
     * we don't necessarily want to show all of those as individual jobs in ops manager as they may merge and join into a single ending flow.
     * For a flood of starting jobs if ops manager receives more than 1 starting event within this given interval it will supress the creation of the next Job
     * Set this to -1L or 0L to bypass and always create a job instance per starting flow file.
     * @return time in millis between start of Job creation.  Set to 0L or -1L to always create a job for every starting event
     */
     Long getTimeBetweenBatchJobs();

     OpsManagerNflow NULL_NFLOW = new OpsManagerNflow() {
        @Override
        public OpsManagerNflow.ID getId() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public NflowType getNflowType() {
            return null;
        }

        @Override
        public boolean isStream() {
            return false;
        }

        @Override
        public Long getTimeBetweenBatchJobs() {
            return 0L;
        }
    };


}
