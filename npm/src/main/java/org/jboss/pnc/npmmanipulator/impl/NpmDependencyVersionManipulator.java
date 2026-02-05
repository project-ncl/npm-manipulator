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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.jboss.pnc.npmmanipulator.api.ManipulationSession;
import org.jboss.pnc.npmmanipulator.api.Manipulator;
import org.jboss.pnc.npmmanipulator.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Manipulator} implementation that can modify an NPM project's dependencies and devDependencies with provided
 * version. Format: -DdependencyOverride.[package_name]=[version] and -DdevDependencyOverride.[package_name]=[version]
 */
public class NpmDependencyVersionManipulator implements Manipulator<NpmResult> {

    /** The separator that's used between the override property and the package name. */
    public static final String OVERRIDE_PROPERTY_SEPARATOR = ".";

    /** The property name to override the dependencies version. */
    public static final String DEPENDENCY_OVERRIDE_PARAM = "dependencyOverride";

    /** The property name to override the development dependencies version. */
    public static final String DEV_DEPENDENCY_OVERRIDE_PARAM = "devDependencyOverride";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, String> dependenciesMap;

    private Map<String, String> devDependenciesMap;

    private ManipulationSession<NpmResult> session;

    /**
     * The default public constructor.
     */
    public NpmDependencyVersionManipulator() {
        dependenciesMap = new LinkedHashMap<>();
        devDependenciesMap = new LinkedHashMap<>();
    }

    /**
     * Constructor used in tests.
     *
     * @param dependenciesMap user provided list of dependency and versions to be overridden
     * @param devDependenciesMap user provided list of dev dependency and versions to be overridden
     */
    public NpmDependencyVersionManipulator(
            Map<String, String> dependenciesMap,
            Map<String, String> devDependenciesMap) {
        this.dependenciesMap = dependenciesMap;
        this.devDependenciesMap = devDependenciesMap;
        this.session = new NpmManipulationSession();
    }

    @Override
    public boolean init(final ManipulationSession<NpmResult> session) {
        this.session = session;

        Properties userProps = session.getUserProps();
        if (userProps != null) {
            // Get the dependencies override versions
            userProps.keySet()
                    .stream()
                    .filter(key -> key.toString().startsWith(DEPENDENCY_OVERRIDE_PARAM + OVERRIDE_PROPERTY_SEPARATOR))
                    .forEach(key -> {
                        String version = userProps.getProperty(key.toString());
                        dependenciesMap.put(
                                key.toString()
                                        .substring((DEPENDENCY_OVERRIDE_PARAM + OVERRIDE_PROPERTY_SEPARATOR).length()),
                                version);
                    });

            // Get the development dependencies override versions
            userProps.keySet()
                    .stream()
                    .filter(
                            key -> key.toString()
                                    .startsWith(DEV_DEPENDENCY_OVERRIDE_PARAM + OVERRIDE_PROPERTY_SEPARATOR))
                    .forEach(key -> {
                        String version = userProps.getProperty(key.toString());
                        devDependenciesMap.put(
                                key.toString()
                                        .substring(
                                                (DEV_DEPENDENCY_OVERRIDE_PARAM + OVERRIDE_PROPERTY_SEPARATOR).length()),
                                version);
                    });

            return !dependenciesMap.isEmpty() || !devDependenciesMap.isEmpty();
        }

        return false;
    }

    @Override
    public Set<Project> applyChanges(final List<Project> projects) throws ManipulationException {

        Set<Project> changed = new HashSet<>();
        for (Project project : projects) {
            if (project instanceof NpmPackage) {
                NpmPackage npmPackage = (NpmPackage) project;

                if (!dependenciesMap.isEmpty()) {
                    Map<String, String> dependencies = npmPackage.getDependencies();
                    dependenciesMap.keySet().forEach(dependency -> {
                        if (dependencies.containsKey(dependency)) {
                            String currentVersion = dependencies.get(dependency);
                            String overrideVersion = dependenciesMap.get(dependency);

                            try {
                                if (!currentVersion.equals(overrideVersion)) {
                                    npmPackage.setDependencyVersion(dependency, dependenciesMap.get(dependency), false);
                                    logger.debug(
                                            "Changing version of dependency `{}` from `{}` to `{}`",
                                            dependency,
                                            currentVersion,
                                            overrideVersion);
                                    session.getResult().getDependenciesMap().put(dependency, overrideVersion);
                                    changed.add(npmPackage);
                                }
                            } catch (ManipulationException ex) {
                                if (logger.isErrorEnabled()) {
                                    logger.error(
                                            "Could not change version of dependency '{}' from '{}' to '{}'",
                                            dependency,
                                            currentVersion,
                                            overrideVersion,
                                            ex);
                                }
                            }
                        }
                    });
                }

                if (!devDependenciesMap.isEmpty()) {
                    Map<String, String> devDependencies = npmPackage.getDevDependencies();
                    devDependenciesMap.keySet().forEach(devDependency -> {
                        if (devDependencies.containsKey(devDependency)) {
                            String currentVersion = devDependencies.get(devDependency);
                            String overrideVersion = devDependenciesMap.get(devDependency);

                            try {
                                if (!currentVersion.equals(overrideVersion)) {
                                    npmPackage.setDependencyVersion(
                                            devDependency,
                                            devDependenciesMap.get(devDependency),
                                            true);
                                    logger.debug(
                                            "Changing version of devDependency `{}` from `{}` to `{}`",
                                            devDependency,
                                            currentVersion,
                                            overrideVersion);
                                    session.getResult()
                                            .getDevDependenciesMap()
                                            .put(devDependency, devDependenciesMap.get(devDependency));
                                    changed.add(npmPackage);
                                }
                            } catch (ManipulationException ex) {
                                if (logger.isErrorEnabled()) {
                                    logger.error(
                                            "Could not change version of devDependency '{}' from '{}' to '{}'",
                                            devDependency,
                                            currentVersion,
                                            overrideVersion,
                                            ex);
                                }
                            }
                        }
                    });
                }
            } else {
                throw new ManipulationException(
                        "Manipulation failed, because project type {} is not supported by NPM manipulation.",
                        project.getClass());
            }
        }
        return changed;
    }

    @Override
    public Collection<Class<? extends Manipulator<NpmResult>>> getManipulatorDependencies() {
        return Collections.emptyList();
    }

}
