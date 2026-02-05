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
package org.jboss.pnc.npmmanipulator.api;

import java.util.List;
import java.util.Properties;

/**
 * The manipulation session supposed to store current state. State consists of both configuration (normally detected
 * from the user properties, or -D options on the command line), and also changes detected in the scan() method
 * invocation that will be applied later.
 *
 * @param <R> the generic type
 */
public interface ManipulationSession<R> {

    /**
     * Provides active manipulators for current session based on provided parameters.
     *
     * @return ordered list of active manipulators
     * @throws ManipulationException in case of an error during initialization
     */
    List<Manipulator<R>> getActiveManipulators() throws ManipulationException;

    /**
     * Provides the list of project files that will be manipulated.
     *
     * @return list of project files
     */
    List<Project> getProjects();

    /**
     * Provides java properties passed on command line.
     *
     * @return parsed properties
     */
    Properties getUserProps();

    void setState(String key, Object state);

    <T> T getState(String key, Class<T> cls);

    R getResult();

    /**
     * Writes manipulation result into result file.
     */
    void writeResult();

}
