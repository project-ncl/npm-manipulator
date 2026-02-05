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

import org.junit.Test;

/**
 * Test class for {@link NpmPackageScopeManipulator}.
 *
 * @author pkocandr
 */
public class NpmPackageScopeManipulatorTest {

    /**
     * Tests that the {@link NpmPackageScopeManipulator#getScopedName(String, String)} returns scoped name with prefix
     * and correct scope-name separator.
     */
    @Test
    public void getScopedNameNoOrigScope() {
        NpmPackageScopeManipulator manipulator = new NpmPackageScopeManipulator("jboss");

        String scopedName = manipulator.getScopedName("manipulator");

        assertThat(scopedName, is("@jboss/manipulator"));
    }

    /**
     * Tests that the {@link NpmPackageScopeManipulator#getScopedName(String, String)} returns scoped name with prefix
     * and correct scope-name separator even when passed-in scope contains the prefix and separator already.
     */
    @Test
    public void getScopedNameNoOrigScopeAcceptAtAndSlash() {
        NpmPackageScopeManipulator manipulator = new NpmPackageScopeManipulator("@jboss/");

        String scopedName = manipulator.getScopedName("manipulator");

        assertThat(scopedName, is("@jboss/manipulator"));
    }

    /**
     * Tests that the {@link NpmPackageScopeManipulator#getScopedName(String, String)} returns scoped name with prefix
     * and correct scope-name separator even when the original name contained different scope.
     */
    @Test
    public void getScopedNameWithOrigScope() {
        NpmPackageScopeManipulator manipulator = new NpmPackageScopeManipulator("@jboss");

        String scopedName = manipulator.getScopedName("@foobar/manipulator");

        assertThat(scopedName, is("@jboss/manipulator"));
    }

}
