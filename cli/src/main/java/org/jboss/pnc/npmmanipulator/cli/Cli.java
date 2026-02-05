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

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.jboss.pnc.npmmanipulator.api.ManipulationSession;
import org.jboss.pnc.npmmanipulator.impl.NpmManipulationSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.ongres.process.FluentProcess;
import com.ongres.process.FluentProcessBuilder;
import com.ongres.process.Output;
import com.redhat.resilience.otel.OTelCLIHelper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

public class Cli {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @SuppressWarnings("rawtypes")
    private ManipulationSession session;

    @SuppressWarnings("rawtypes")
    private final ManipulationManager manipulationManager = new ManipulationManager<>();

    /** Properties a user may define on the command line. */
    private Properties userProps;

    public static void main(String[] args) {
        System.exit(new Cli().run(args));
    }

    @SuppressWarnings("unchecked")
    public int run(String[] args) {
        Options options = new Options();
        options.addOption("h", false, "Print this help message.");
        options.addOption(
                Option.builder("t")
                        .longOpt("type")
                        .desc(
                                "The project type. Can be only NPM for now and is not mandatory. It is not case-sensitive.")
                        .build());
        options.addOption(Option.builder("d").longOpt("debug").desc("Enable debug").build());
        options.addOption(Option.builder("c").longOpt("trace").desc("Enable trace").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Print help").build());
        options.addOption(
                Option.builder("f").longOpt("file").hasArgs().numberOfArgs(1).desc("Project definition file").build());
        options.addOption(
                Option.builder("r")
                        .longOpt("result")
                        .hasArgs()
                        .numberOfArgs(1)
                        .desc(
                                "Json file to be generated at the end of manipulation containing the results. Is not mandatory.")
                        .build());
        options.addOption(Option.builder().longOpt("log-context").desc("Add log-context ID").numberOfArgs(1).build());
        options.addOption(
                Option.builder("l").longOpt("log").desc("Log file to output logging to").numberOfArgs(1).build());
        options.addOption(
                Option.builder("D").hasArgs().numberOfArgs(2).valueSeparator('=').desc("Java Properties").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.debug("Caught problem parsing ", e);
            System.err.println(e.getMessage());

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("...", options);
            return 10;
        }

        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("...", options);
            System.exit(0);
        }
        if (cmd.hasOption('D')) {
            userProps = cmd.getOptionProperties("D");
        }

        File projectFile;
        if (cmd.hasOption('f')) {
            projectFile = new File(cmd.getOptionValue('f'));
        } else {
            projectFile = new File(System.getProperty("user.dir"));
        }

        File result = null;
        if (cmd.hasOption('r')) {
            result = new File(cmd.getOptionValue('r'));
        }

        if (cmd.hasOption("log-context")) {
            String mdc = cmd.getOptionValue("log-context");
            if (isNotEmpty(mdc)) {
                // Append a space to split up level and log-context markers.
                MDC.put("LOG-CONTEXT", mdc + ' ');
            }
        }

        createSession(projectFile, result);

        final Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) rootLogger;

        if (cmd.hasOption('l')) {
            if (runningInContainer()) {
                logger.warn("Disabling log file as running in container!");
            } else {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                loggerContext.reset();

                PatternLayoutEncoder ple = new PatternLayoutEncoder();
                ple.setPattern("%mdc{LOG-CONTEXT}%level %logger{36} %msg%n");
                ple.setContext(loggerContext);
                ple.start();

                FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
                fileAppender.setEncoder(ple);
                fileAppender.setContext(loggerContext);
                fileAppender.setName("fileLogging");
                fileAppender.setAppend(false);
                fileAppender.setFile(cmd.getOptionValue("l"));
                fileAppender.start();

                root.addAppender(fileAppender);
                root.setLevel(Level.INFO);
            }
        }
        // Set debug logging after session creation
        if (cmd.hasOption('d')) {
            root.setLevel(Level.DEBUG);
        }
        if (cmd.hasOption('c')) {
            root.setLevel(Level.TRACE);
        }

        if (!projectFile.exists()) {
            logger.info("NPM Manipulation failed. File {} cannot be found.", projectFile);
            return 10;
        }

        try {
            if (userProps.containsKey("preScript")) {
                // Value is a comma separated list of URLs
                resolveScripts(userProps.getProperty("preScript").split(",")).forEach(this::executeScript);
            }

            String endpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
            String service = System.getenv("OTEL_SERVICE_NAME");
            if (endpoint != null) {
                if (service == null) {
                    service = "npm-project-manipulator";
                }
                logger.info("Enabling OpenTelemetry collection on {} with service name {}", endpoint, service);
                OTelCLIHelper.startOTel(
                        service,
                        "cli",
                        OTelCLIHelper.defaultSpanProcessor(OTelCLIHelper.defaultSpanExporter(endpoint)));
            }
            manipulationManager.init(session);
            manipulationManager.scanAndApply(session);

            if (userProps.containsKey("postScript")) {
                // Value is a comma separated list of URLs
                resolveScripts(userProps.getProperty("postScript").split(",")).forEach(this::executeScript);
            }
        } catch (ManipulationException ex) {
            logger.error("Project Manipulation failed; original error is: {}", ex.getMessage());
            logger.debug("Project Manipulation error trace is", ex);
            return 10;
        } catch (Exception ex) {
            logger.error("Project Manipulation failed.", ex);
            return 100;
        } finally {
            OTelCLIHelper.stopOTel();
        }
        return 0;
    }

    private void createSession(File projectFile, File resultFile) {
        session = NpmManipulationSessionFactory
                .createSession(projectFile, resultFile, System.getProperties(), userProps);
    }

    /**
     * Determine whether the process is running inside an image. See
     * <a href="https://hackmd.io/gTlORH1KTuOuoWoAAzD42g">here</a>
     *
     * @return true if running in a container
     */
    private boolean runningInContainer() {
        final Path cgroup = Paths.get("/proc/1/cgroup");
        boolean result = false;

        if (Files.isReadable(cgroup)) {
            try (Stream<String> stream = Files.lines(cgroup)) {
                result = stream.anyMatch(line -> line.contains("docker") || line.contains("kubepods"));
            } catch (IOException e) {
                logger.error("Unable to determine if running in a container", e);
            }
        }

        if (!result) {
            result = System.getenv().containsKey("container");
        }

        return result;
    }

    List<File> resolveScripts(String[] scripts) throws IOException {
        final List<File> results = new ArrayList<>(scripts.length);
        for (final String script : scripts) {
            logger.info("Attempting to read URL {}", script);
            URL ref = new URL(script);
            File result;
            if (!"file".equals(ref.getProtocol())) {
                result = Files.createTempFile(UUID.randomUUID().toString(), null).toFile();
                FileUtils.copyURLToFile(ref, result);
            } else {
                result = new File(ref.getPath());
            }
            results.add(result);
        }
        return results;
    }

    void executeScript(File resolvedScript) {
        // https://gitlab.com/ongresinc/fluent-process
        logger.info("Executing script {}", resolvedScript);
        FluentProcessBuilder builder = new FluentProcessBuilder(resolvedScript.toString())
                .allowedExitCode(0)
                .dontCloseAfterLast();
        try (FluentProcess process = builder.start()) {
            Output output = process.tryGet();
            if (output.error().isPresent()) {
                logger.error(output.error().get());
            }
            if (output.output().isPresent()) {
                logger.info(output.output().get());
            }
            if (output.exception().isPresent()) {
                throw new RuntimeException("Problem executing script", output.exception().get());
            }
        }
    }
}
