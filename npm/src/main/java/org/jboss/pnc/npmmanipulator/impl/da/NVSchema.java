/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018-2026 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.npmmanipulator.impl.da;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public class NVSchema {

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public String versionFilter;

    /**
     * The mode telling DA which packages to select.
     */
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public String mode;

    public List<Map<String, Object>> packages;

    /**
     * Flag telling DA to not filter the results by quality levels. This is useful to get all versions when incrementing
     * the built package version.
     */
    public boolean includeAll;

    public NVSchema() {
    }

    public NVSchema(String versionFilter, String mode, boolean includeAll, List<Map<String, Object>> packages) {
        this.includeAll = includeAll;
        this.versionFilter = versionFilter;
        this.mode = mode;
        this.packages = packages;
    }

    @Override
    public String toString() {
        return "NVSchema(includeAll '" + includeAll + "' :: versionFilter '" + versionFilter + "' :: mode '" + mode
                + "' :: packages " + packages + ")";
    }
}
