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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.jboss.pnc.npmmanipulator.api.ManipulationSession;
import org.jboss.pnc.npmmanipulator.api.Manipulator;
import org.jboss.pnc.npmmanipulator.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class NpmManipulationSession implements ManipulationSession<NpmResult> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Properties properties;
    private File pkg;
    private File resultFIle;
    private Properties userProps;
    private List<Manipulator<NpmResult>> manipulators;
    private final Map<String, Object> states = new HashMap<>();
    private NpmResult result = new NpmResult();

    /** Default constructor with limited visibility for tests. */
    NpmManipulationSession() {
    }

    public NpmManipulationSession(File pkg, File resultFile, Properties properties, Properties userProps) {
        this.pkg = pkg;
        this.resultFIle = resultFile;
        this.properties = properties;
        this.userProps = userProps;
    }

    @Override
    public List<Manipulator<NpmResult>> getActiveManipulators() throws ManipulationException {
        if (manipulators == null) {
            manipulators = new ArrayList<>();

            @SuppressWarnings("unchecked")
            Manipulator<NpmResult>[] allManipulators = new Manipulator[] {
                    new NpmPackageVersionManipulator(),
                    new NpmPackageScopeManipulator(),
                    new NpmDependencyVersionManipulator(),
                    new DAVersionsCollector() };
            for (Manipulator<NpmResult> manipulator : allManipulators) {
                if (manipulator.init(this)) {
                    manipulators.add(manipulator);
                }
            }
        }
        return manipulators;
    }

    @Override
    public List<Project> getProjects() {
        List<Project> projects = new ArrayList<>();

        File packageLock = null;
        File packageFile = null;
        String packageDirPath = null;

        if (pkg.exists()) {
            if (pkg.isFile()) {
                if ("package.json".equals(pkg.getName())) {
                    packageFile = pkg;
                } else if ("npm-shrinkwrap.json".equals(pkg.getName())) {
                    packageLock = pkg;
                } else if ("package-lock.json".equals(pkg.getName())) {
                    packageLock = pkg;
                }

                packageDirPath = pkg.getParent();
            } else {
                packageDirPath = pkg.getPath();
            }

            if (packageFile == null) {
                packageFile = new File(packageDirPath + File.separator + "package.json");
            }
            if (packageLock == null) {
                packageLock = new File(packageDirPath + File.separator + "npm-shrinkwrap.json");
                if (!packageLock.exists()) {
                    packageLock = new File(packageDirPath + File.separator + "package-lock.json");
                    if (!packageLock.exists()) {
                        packageLock = null;
                    }
                }
            }

            NpmPackageImpl pack = new NpmPackageImpl(packageFile, packageLock);
            projects.add(pack);

            try {
                result.setName(pack.getName());
                result.setVersion(pack.getVersion());
            } catch (ManipulationException e) {
                throw new IllegalArgumentException(
                        "The project data could not be read from the package file " + pkg + "\nError: "
                                + e.getMessage(),
                        e);
            }
        } else {
            logger.error("Given package path {} does not exist.", pkg);
        }

        return projects;
    }

    public Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }

    public File getTarget() {
        return pkg;
    }

    @Override
    public Properties getUserProps() {
        if (userProps == null) {
            userProps = new Properties();
        }
        return userProps;
    }

    @Override
    public void setState(String key, Object state) {
        states.put(key, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getState(String key, Class<T> cls) {
        return (T) states.get(key);
    }

    @Override
    public NpmResult getResult() {
        return result;
    }

    @Override
    public void writeResult() {
        if (resultFIle != null) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            try {
                writer.writeValue(resultFIle, result);
            } catch (IOException ex) {
                logger.error("Error when writing result file: " + ex.getMessage(), ex);
                ;
            }
        }
    }

}
