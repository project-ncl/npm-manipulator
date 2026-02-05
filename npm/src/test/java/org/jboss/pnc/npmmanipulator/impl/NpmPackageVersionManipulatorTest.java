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
/**
 *
 */
package org.jboss.pnc.npmmanipulator.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.jboss.pnc.npmmanipulator.api.Project;
import org.junit.Test;

/**
 * Test class for {@link NpmPackageVersionManipulator}.
 *
 * @author pkocandr
 */
public class NpmPackageVersionManipulatorTest {

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 0 when the
     * available version set is empty.
     */
    @Test
    public void findHighestIncrementalNumHyphenedWithNoAvailableVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.HYPHENED,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(0, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 0 when the
     * available version set is not empty, but contains only versions with non-matching major-minor-patch combination or
     * with matching one, but non-matching suffix.
     */
    @Test
    public void findHighestIncrementalNumHyphenedWithNoMatchingAvailableVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.HYPHENED,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.1-jboss-1");
        availableSet.add("1.0.0-ncl-1");
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(0, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 2 when the
     * available version set is not empty and contains a version with non-matching suffix and 2 matching ones with 00001
     * and 00002 after it.
     */
    @Test
    public void findHighestIncrementalNumHyphenedWithMatchingAvailableVersion() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.HYPHENED,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-00001");
        availableSet.add("1.0.0-jboss-00002");
        availableSet.add("1.0.0-ncl-1");
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(2, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 0 when the
     * available version set is empty.
     */
    @Test
    public void findHighestIncrementalNumSemverWithNoAvailableVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.SEMVER,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(0, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 0 when the
     * available version set is not empty, but contains only versions with non-matching major-minor-patch combination or
     * with matching one, but non-matching suffix.
     */
    @Test
    public void findHighestIncrementalNumSemverWithNoMatchingAvailableVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.SEMVER,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.1-jboss.1");
        availableSet.add("1.0.0-ncl.1");
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(0, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 2 when the
     * available version set is not empty and contains a version with non-matching suffix and 2 matching ones with 00001
     * and 00002 after it.
     */
    @Test
    public void findHighestIncrementalNumSemverWithMatchingAvailableVersion() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.SEMVER,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss.1");
        availableSet.add("1.0.0-jboss.2");
        availableSet.add("1.0.0-ncl.1");
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(2, is(highestNum));
    }

    /**
     * Tests generation of new version when there is no pre-existing suffixed version. For version 1.0.0 it expects to
     * get 1.0.0-jboss-00001.
     */
    @Test
    public void generateNewHyphenedVersionWhenNoSuffixedExists() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.HYPHENED,
                "jboss",
                5,
                null,
                null,
                null);

        String newVersion = manipulator.generateNewHyphenedVersion("1.0.0", Collections.emptySet());

        assertThat("1.0.0-jboss-00001", is(newVersion));
    }

    /**
     * Tests generation of new version when there are pre-existing suffixed versions - jboss-1 and jboss-00002. For
     * version 1.0.0 it expects to get 1.0.0-jboss-00003.
     */
    @Test
    public void generateNewHyphenedVersionWithAvailableSuffixedVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.HYPHENED,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        availableSet.add("1.0.0-jboss-00002");
        String newVersion = manipulator.generateNewHyphenedVersion("1.0.0", availableSet);

        assertThat("1.0.0-jboss-00003", is(newVersion));
    }

    /**
     * Tests generation of new version for version that already contains the suffix when there are pre-existing suffixed
     * versions - jboss-1 and jboss-00002. It expects that the original suffix will be removed and replaced by the
     * generated one based on the highest available version, so for version 1.0.0-jboss-00004 it expects to get
     * 1.0.0-jboss-00003.
     */
    @Test
    public void generateNewHyphenedVersionForSuffixedVersionWithPreexistingSuffixedVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.HYPHENED,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        availableSet.add("1.0.0-jboss-00002");
        String newVersion = manipulator.generateNewHyphenedVersion("1.0.0-jboss-00004", availableSet);

        assertThat("1.0.0-jboss-00003", is(newVersion));
    }

    /**
     * Tests generation of new version when there is no pre-existing suffixed version. For version 1.0.0 it expects to
     * get 1.0.0-jboss-00001.
     */
    @Test
    public void generateNewSemverVersionWhenNoSuffixedExists() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.SEMVER,
                "jboss",
                5,
                null,
                null,
                null);

        String newVersion = manipulator.generateNewSemverVersion("1.0.0", Collections.emptySet());

        assertThat(newVersion, is("1.0.0-jboss.1"));
    }

    /**
     * Tests generation of new version when there are pre-existing suffixed versions - jboss-1 and jboss-00002. For
     * version 1.0.0 it expects to get 1.0.0-jboss-00003.
     */
    @Test
    public void generateNewSemverVersionWithAvailableSuffixedVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.SEMVER,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss.2");
        String newVersion = manipulator.generateNewSemverVersion("1.0.0", availableSet);

        assertThat(newVersion, is("1.0.0-jboss.3"));
    }

    /**
     * Tests generation of new version for version that already contains the suffix when there are pre-existing suffixed
     * versions - jboss-1 and jboss-00002. It expects that the original suffix will be removed and replaced by the
     * generated one based on the highest available version, so for version 1.0.0-jboss-00004 it expects to get
     * 1.0.0-jboss-00003.
     */
    @Test
    public void generateNewSemverVersionForSuffixedVersionWithPreexistingSuffixedVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                NpmPackageVersionManipulator.VersioningStrategy.SEMVER,
                "jboss",
                5,
                null,
                null,
                null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss.2");
        String newVersion = manipulator.generateNewSemverVersion("1.0.0-jboss.4", availableSet);

        assertThat(newVersion, is("1.0.0-jboss.3"));
    }

    /**
     * Tests getting new version when the complete version is set to an override. It sets also versionSuffix,
     * versionBaseOverride and versionSuffixOverride, which should be all ignored and the result should be only the
     * overriden version.
     *
     * @throws ManipulationException in case of an error
     */
    @Test
    public void getNewVersionOverride() throws ManipulationException {
        String versionOverride = "2.0.0-foo-001";
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                null,
                "jboss",
                5,
                "3.0.0",
                "bar-02",
                versionOverride);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        String newVersion = manipulator.getNewVersion("1.0.0", availableSet);

        assertThat(newVersion, is(versionOverride));
    }

    /**
     * Tests applying the version update with an override. The result should be only the overriden version.
     *
     * @throws ManipulationException in case of an error
     */
    @Test
    public void applyChangesWithVersionOverride() throws ManipulationException {
        String versionOverride = "2.0.0-foo-001";
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                null,
                null,
                null,
                null,
                null,
                versionOverride);

        List<Project> projects = new ArrayList<>();
        projects.add(new NpmPackage() {
            private String name = "test";
            private String version = "1.0.0";

            @Override
            public void update() throws ManipulationException {
                // do nothing
            }

            @Override
            public String getName() throws ManipulationException {
                return name;
            }

            @Override
            public String getVersion() throws ManipulationException {
                return version;
            }

            @Override
            public void setName(String name) throws ManipulationException {
                this.name = name;
            }

            @Override
            public void setVersion(String version) throws ManipulationException {
                this.version = version;
            }

            @Override
            public Map<String, String> getDependencies() throws ManipulationException {
                return Collections.emptyMap();

            }

            @Override
            public Map<String, String> getDevDependencies() throws ManipulationException {
                return Collections.emptyMap();
            }

            @Override
            public void setDependencyVersion(String dependencyName, String version, boolean isDevelopment)
                    throws ManipulationException {
            }

        });
        Set<Project> changed = manipulator.applyChanges(projects);

        assertThat(changed.size(), is(1));
        assertThat(((NpmPackage) changed.iterator().next()).getVersion(), is(versionOverride));
    }

}
