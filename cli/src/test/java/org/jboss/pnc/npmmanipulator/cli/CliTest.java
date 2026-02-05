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
package org.jboss.pnc.npmmanipulator.cli;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.org.webcompere.systemstubs.SystemStubs.tapSystemErrAndOut;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CliTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testResolveScripts() throws IOException {
        File target = folder.newFile();
        Cli cli = new Cli();

        String[] toTest = new String[2];
        toTest[0] = "file://" + target.getAbsolutePath();
        toTest[1] = "https://raw.githubusercontent.com/project-ncl/npm-manipulator/refs/heads/master/README.md";

        List<File> result = cli.resolveScripts(toTest);
        assertEquals(result.get(0), target);
        assertTrue(
                FileUtils.readFileToString(result.get(1), Charset.defaultCharset())
                        .contains(
                                "Various manipulations can be performed on a project and their execution can be controlled by provided arguments"));
    }

    @Test
    public void testExecuteScript() throws Exception {
        File target = folder.newFile();
        //noinspection ResultOfMethodCallIgnored
        target.setExecutable(true);
        FileUtils.writeStringToFile(
                target,
                "#!/bin/sh\necho \"### HELLO!\"\n",
                Charset.defaultCharset());
        Cli cli = new Cli();
        String text = tapSystemErrAndOut(() -> cli.executeScript(target));
        assertTrue(text.contains("### HELLO!"));
    }

    @Test
    public void testExecuteScriptWithFailures() throws Exception {
        File target = folder.newFile();
        //noinspection ResultOfMethodCallIgnored
        target.setExecutable(true);
        FileUtils.writeStringToFile(
                target,
                "#!/bin/sh\necho \"### HELLO!\"\nexit 1\n",
                Charset.defaultCharset());
        Cli cli = new Cli();
        String text = tapSystemErrAndOut(() -> {
            try {
                cli.executeScript(target);
                fail("No exception thrown");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("Problem executing script"));
            }
        });
        assertTrue(text.contains("### HELLO!"));
    }
}