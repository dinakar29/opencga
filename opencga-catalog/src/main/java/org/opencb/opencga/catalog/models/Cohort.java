/*
 * Copyright 2015 OpenCB
 *
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
 */

package org.opencb.opencga.catalog.models;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 *         <p>
 *         Set of samples grouped according to criteria
 */
public class Cohort {

    private long id;
    private String name;
    private Type type;
    private String creationDate;
    private Status status;
    private CohortStatus cohortStatus;
    private String description;

    private List<Long> samples;

    private Map<String, Object> stats;
    private Map<String, Object> attributes;

    public Cohort() {
    }

    public Cohort(String name, Type type, String creationDate, String description, List<Long> samples,
                  Map<String, Object> attributes) {
        this(-1, name, type, creationDate, CohortStatus.NONE, description, samples, Collections.emptyMap(), attributes);
    }

    public Cohort(int id, String name, Type type, String creationDate, CohortStatus cohortStatus, String description, List<Long> samples,
                  Map<String, Object> stats, Map<String, Object> attributes) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.creationDate = creationDate;
        this.cohortStatus = cohortStatus;
        this.status = new Status();
        this.description = description;
        this.samples = samples;
        this.stats = stats;
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Cohort{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", type=").append(type);
        sb.append(", creationDate='").append(creationDate).append('\'');
        sb.append(", cohortStatus=").append(cohortStatus);
        sb.append(", status=").append(status);
        sb.append(", description='").append(description).append('\'');
        sb.append(", samples=").append(samples);
        sb.append(", stats=").append(stats);
        sb.append(", attributes=").append(attributes);
        sb.append('}');
        return sb.toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public CohortStatus getCohortStatus() {
        return cohortStatus;
    }

    public void setCohortStatus(CohortStatus cohortStatus) {
        this.cohortStatus = cohortStatus;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Long> getSamples() {
        return samples;
    }

    public void setSamples(List<Long> samples) {
        this.samples = samples;
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public enum CohortStatus {
        NONE,
        CALCULATING,
        READY,
        INVALID
    }

    //Represents the criteria of grouping samples in the cohort
    public enum Type {
        CASE_CONTROL,
        CASE_SET,
        CONTROL_SET,
        PAIRED,
        PAIRED_TUMOR,
        FAMILY,
        TRIO,
        COLLECTION
    }
}
