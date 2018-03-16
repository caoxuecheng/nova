/**
 *
 */
package com.onescorpin.metadata.api.event.nflow;

/*-
 * #%L
 * onescorpin-metadata-api
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

import com.onescorpin.metadata.api.event.MetadataChange;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.Nflow.ID;
import com.onescorpin.metadata.api.nflow.Nflow.State;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 *
 */
public class NflowChange extends MetadataChange {

    private static final long serialVersionUID = 1L;

    private final Nflow.ID nflowId;
    private final String nflowName;
    private final Nflow.State nflowState;



    public NflowChange(ChangeType change, ID nflowId, State nflowState) {
        this(change, "", nflowId, nflowState);
    }

    public NflowChange(ChangeType change, String descr, ID nflowId, State nflowState) {
        this(change,descr,null,nflowId,nflowState);
    }

    public NflowChange(ChangeType change, String descr, String nflowName,ID nflowId, State nflowState) {
        super(change, descr);
        this.nflowId = nflowId;
        this.nflowState = nflowState;
        this.nflowName = StringUtils.isBlank(nflowName)? null : nflowName;
    }

    public Nflow.ID getNflowId() {
        return nflowId;
    }

    public Nflow.State getNflowState() {
        return nflowState;
    }

    public Optional<String> getNflowName() {
        return Optional.ofNullable(nflowName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.nflowState, this.nflowId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NflowChange) {
            NflowChange that = (NflowChange) obj;
            return super.equals(that) &&
                   Objects.equals(this.nflowId, that.nflowId) &&
                   Objects.equals(this.nflowState, that.nflowState);
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Nflow change ");
        return sb
            .append("(").append(getChange()).append(") - ")
            .append("ID: ").append(this.nflowId)
            .append(" nflow state: ").append(this.nflowState)
            .toString();

    }
}
