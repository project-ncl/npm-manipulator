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
package org.jboss.pnc.npmmanipulator.impl;

import java.util.Map;

import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.jboss.pnc.npmmanipulator.api.Project;

public interface NpmPackage extends Project {

    /**
     * Retrieves package name from loaded package file.
     *
     * @return retrieved name
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    String getName() throws ManipulationException;

    /**
     * Updates package name in the loaded package file and package-lock file.
     *
     * @param name the name to be set
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    void setName(String name) throws ManipulationException;

    /**
     * Retrieves package version from loaded package file.
     *
     * @return retrieved version
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    String getVersion() throws ManipulationException;

    /**
     * Updates package version in the loaded package file and package-lock file.
     *
     * @param version the version to be set
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    void setVersion(String version) throws ManipulationException;

    /**
     * Retrieves the dependencies from loaded package file.
     *
     * @return retrieved dependencies
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    Map<String, String> getDependencies() throws ManipulationException;

    /**
     * Retrieves the devDependencies from loaded package file.
     *
     * @return retrieved devDependencies
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    Map<String, String> getDevDependencies() throws ManipulationException;

    /**
     * Updates the dependency version in the loaded package file.
     *
     * @param dependencyName the name of the dependency to be changed
     * @param version the version to be set
     * @param isDevelopment whether the change needs to be applied in the `dependencies` or `devDependencies` list
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    void setDependencyVersion(String dependencyName, String version, boolean isDevelopment)
            throws ManipulationException;

}
