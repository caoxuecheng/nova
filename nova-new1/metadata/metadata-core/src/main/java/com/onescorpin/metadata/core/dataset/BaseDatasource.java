/**
 *
 */
package com.onescorpin.metadata.core.dataset;

/*-
 * #%L
 * onescorpin-metadata-core
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

import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowSource;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class BaseDatasource implements Datasource {

    private ID id;
    private String name;
    private String description;
    private DateTime creationTime;
    private Set<NflowSource> nflowSources = new HashSet<>();
    private Set<NflowDestination> nflowDestinations = new HashSet<>();

    public BaseDatasource(String name, String descr) {
        this.id = new DatasourceId();
        this.creationTime = new DateTime();
        this.name = name;
        this.description = descr;
    }

    public ID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DateTime getCreatedTime() {
        return creationTime;
    }

    @Override
    public Set<NflowSource> getNflowSources() {
        return this.nflowSources;
    }

    @Override
    public Set<NflowDestination> getNflowDestinations() {
        return this.nflowDestinations;
    }


    protected static class DatasourceId implements ID {

        private UUID uuid = UUID.randomUUID();

        public DatasourceId() {
        }

        public DatasourceId(Serializable ser) {
            if (ser instanceof String) {
                this.uuid = UUID.fromString((String) ser);
            } else if (ser instanceof UUID) {
                this.uuid = (UUID) ser;
            } else {
                throw new IllegalArgumentException("Unknown ID value: " + ser);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DatasourceId) {
                DatasourceId that = (DatasourceId) obj;
                return Objects.equals(this.uuid, that.uuid);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass(), this.uuid);
        }

        @Override
        public String toString() {
            return this.uuid.toString();
        }
    }
}
