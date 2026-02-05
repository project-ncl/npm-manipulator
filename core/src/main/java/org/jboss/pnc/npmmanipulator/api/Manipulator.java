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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents one way that a project may be manipulated/modified during pre-processing. State is stored in the
 * {@link ManipulationSession} instance. State consists of both configuration (normally detected from the user
 * properties, or -D options on the command line), and also changes detected in the scan() method invocation that will
 * be applied later.
 *
 * Note that the order of the Manipulators is important. While later Manipulators such as the Remove* are not so
 * important in terms of order, the initial ones are.
 */
public interface Manipulator<R> {

    /**
     * Initialize any state for the manipulator and checks if it can be ran with current setup.
     *
     * @param session the session to initialize with.
     * @return true if the manipulator will be ran
     * @throws ManipulationException if an error occurs.
     */
    boolean init(ManipulationSession<R> session) throws ManipulationException;

    /**
     * Apply any changes to the project definitions related to the given list of {@link Project} instances.
     *
     * @param projects the Projects to apply the changes to.
     * @return the set of changed projects.
     * @throws ManipulationException if an error occurs.
     */
    Set<Project> applyChanges(List<Project> projects) throws ManipulationException;

    /**
     * Gets dependencies (preceding manipulations) that has to finish their job before this manipulation can be ran.
     *
     * @return collection of dependencies' classes
     */
    Collection<Class<? extends Manipulator<R>>> getManipulatorDependencies();

}
