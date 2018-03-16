package com.onescorpin.metadata.rest.model.data;

/*-
 * #%L
 * onescorpin-metadata-rest-model
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.security.rest.model.EntityAccessControl;
import com.onescorpin.security.rest.model.User;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@SuppressWarnings("serial")
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY)
@JsonSubTypes({
                  @JsonSubTypes.Type(DirectoryDatasource.class),
                  @JsonSubTypes.Type(HiveTableDatasource.class),
                  @JsonSubTypes.Type(DerivedDatasource.class),
                  @JsonSubTypes.Type(JdbcDatasource.class)
              }
)
public class Datasource extends EntityAccessControl implements com.onescorpin.metadata.datasource.Datasource, Serializable {

    @JsonSerialize(using = DateTimeSerializer.class)
    private DateTime creationTime;

    private String id;
    private String name;
    private String description;
    private boolean encrypted;
    private boolean compressed;
    private Set<Nflow> sourceForNflows = new HashSet<>();
    private Set<Nflow> destinationForNflows = new HashSet<>();


    public Datasource() {
        super();
    }

    public Datasource(String name) {
        super();
        this.name = name;
    }

    public Datasource(String id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public void setOwner(final String owner) {
        final User user = new User();
        user.setSystemName(owner);
        setOwner(user);
    }

    public DateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(DateTime creationTime) {
        this.creationTime = creationTime;
    }

    public Set<Nflow> getSourceForNflows() {
        if (sourceForNflows == null) {
            sourceForNflows = new HashSet<>();
        }
        return sourceForNflows;
    }

    public Set<Nflow> getDestinationForNflows() {
        if (destinationForNflows == null) {
            destinationForNflows = new HashSet<>();
        }
        return destinationForNflows;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

}
