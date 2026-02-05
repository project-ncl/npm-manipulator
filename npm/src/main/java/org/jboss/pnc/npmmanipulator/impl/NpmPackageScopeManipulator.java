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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.jboss.pnc.npmmanipulator.api.ManipulationSession;
import org.jboss.pnc.npmmanipulator.api.Manipulator;
import org.jboss.pnc.npmmanipulator.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Manipulator} implementation that can modify an NPM package scope.
 */
public class NpmPackageScopeManipulator implements Manipulator<NpmResult> {

    /** The prefix prepended before the scope string. */
    public static final String SCOPE_PREFIX = "@";

    /** The separator that's used between the package scope and package name. */
    public static final String SCOPE_NAME_SEPARATOR = "/";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** The package scope to be added. */
    private String packageScope;

    /** The manipulation session. */
    private ManipulationSession<NpmResult> session;

    /** The default public constructor. */
    public NpmPackageScopeManipulator() {
    }

    /**
     * Constructor used in tests.
     *
     * @param packageScope the package scope to be added
     */
    NpmPackageScopeManipulator(String packageScope) {
        this.packageScope = packageScope;
        this.session = new NpmManipulationSession();
    }

    @Override
    public boolean init(final ManipulationSession<NpmResult> session) {
        this.session = session;

        Properties userProps = session.getUserProps();
        if (userProps != null) {
            packageScope = userProps.getProperty("packageScope");

            return !isEmpty(packageScope);
        }

        return false;
    }

    @Override
    public Set<Project> applyChanges(final List<Project> projects) throws ManipulationException {
        Set<Project> changed = new HashSet<>();
        for (Project project : projects) {
            if (project instanceof NpmPackage) {
                NpmPackage npmPackage = (NpmPackage) project;

                String origName = npmPackage.getName();
                String newName = getScopedName(origName);

                if (!origName.equals(newName)) {
                    logger.info("Adding package scope: {} -> {}", origName, newName);
                    npmPackage.setName(newName);
                    session.getResult().setName(newName);
                    changed.add(npmPackage);
                }
            } else {
                throw new ManipulationException(
                        "Manipulation failed, because project type {} is not supported by NPM manipulation.",
                        project.getClass());
            }
        }
        return changed;
    }

    /**
     * Generates scoped name from original name and new scope. Strips an existing scope from the original name if any
     * and appends the new one.
     *
     * @param origName the original package name potentially with some previous scope
     * @return the generated scoped name
     */
    String getScopedName(String origName) {
        // strip the existing scope if any
        String strippedName;
        if (origName.startsWith(SCOPE_PREFIX) && origName.contains(SCOPE_NAME_SEPARATOR)) {
            strippedName = origName.split(SCOPE_NAME_SEPARATOR, 2)[1];
        } else {
            strippedName = origName;
        }

        // prepend the new scope
        String newName;
        if (packageScope.endsWith(SCOPE_NAME_SEPARATOR)) {
            newName = packageScope + strippedName;
        } else {
            newName = packageScope + SCOPE_NAME_SEPARATOR + strippedName;
        }

        // ensure the format is correct
        if (!newName.startsWith(SCOPE_PREFIX)) {
            newName = SCOPE_PREFIX + newName;
        }

        return newName;
    }

    @Override
    public Collection<Class<? extends Manipulator<NpmResult>>> getManipulatorDependencies() {
        return Collections.emptyList();
    }

}
