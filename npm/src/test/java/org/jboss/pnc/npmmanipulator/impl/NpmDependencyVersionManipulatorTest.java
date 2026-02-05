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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.jboss.pnc.npmmanipulator.api.Project;
import org.junit.Test;

/**
 * Test class for {@link NpmDependencyVersionManipulatorTest}.
 *
 * @author avibelli
 */
public class NpmDependencyVersionManipulatorTest {

    /**
     * Tests applying the dependency version update with an override.
     *
     * @throws ManipulationException in case of an error
     */
    @Test
    public void applyChangesWithDependencyOverride() throws ManipulationException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("package.json");
        File packageJson = new File(url.getPath());

        assertTrue(packageJson.exists());
        assertTrue(packageJson.isFile());

        Map<String, String> userDependenciesMap = new LinkedHashMap<>();
        userDependenciesMap.put("archiver", "1.2.0");
        userDependenciesMap.put("async", "1.5.2");
        userDependenciesMap.put("body-parser", "1.18.2");
        userDependenciesMap.put("cors", "2.7.1");
        userDependenciesMap.put("express", "4.16.3");
        userDependenciesMap.put("express-bunyan-logger", "1.3.3");
        userDependenciesMap.put("keycloak-admin-client", "^0.12.0");

        Map<String, String> userDevDependenciesMap = new LinkedHashMap<>();
        userDevDependenciesMap.put("deep-equal", "~1.0.1");
        userDevDependenciesMap.put("express", "4.16.5");
        userDevDependenciesMap.put("grunt", "~1.0.1");
        userDevDependenciesMap.put("grunt-fh-build", "~2.0.0");
        userDevDependenciesMap.put("istanbul", "0.4.5-redhat-00001");

        NpmDependencyVersionManipulator manipulator = new NpmDependencyVersionManipulator(
                userDependenciesMap,
                userDevDependenciesMap);

        List<Project> projects = new ArrayList<>();
        NpmPackage npmPackage = new NpmPackageImpl(packageJson, null);

        assertEquals("pnc-example", npmPackage.getName());
        assertEquals("2.0.0-BUILD-NUMBER", npmPackage.getVersion());
        assertEquals(5, npmPackage.getDependencies().size());
        assertEquals(5, npmPackage.getDevDependencies().size());

        projects.add(npmPackage);

        Set<Project> changed = manipulator.applyChanges(projects);

        assertThat(1, is(changed.size()));
        NpmPackage changedProject = (NpmPackage) changed.iterator().next();

        assertEquals(changedProject.getDependencies().size(), 5);
        assertEquals(changedProject.getDevDependencies().size(), 5);
        // Overridden
        assertEquals("1.2.0", changedProject.getDependencies().get("archiver"));
        assertEquals("2.7.1", changedProject.getDependencies().get("cors"));
        assertEquals("1.3.3", changedProject.getDependencies().get("express-bunyan-logger"));
        assertEquals("^0.12.0", changedProject.getDependencies().get("keycloak-admin-client"));

        // Not overridden
        assertEquals("4.16.3", changedProject.getDependencies().get("express"));

        // Not existing (user provided but not in package.json)
        assertNull(changedProject.getDependencies().get("async"));
        assertNull(changedProject.getDependencies().get("body-parser"));

        // Overridden
        assertEquals("4.16.5", changedProject.getDevDependencies().get("express"));
        assertEquals("~1.0.1", changedProject.getDevDependencies().get("grunt"));
        assertEquals("~2.0.0", changedProject.getDevDependencies().get("grunt-fh-build"));
        assertEquals("0.4.5-redhat-00001", changedProject.getDevDependencies().get("istanbul"));

        // Not overridden
        assertEquals("~1.0.1", changedProject.getDevDependencies().get("deep-equal"));
    }
}
