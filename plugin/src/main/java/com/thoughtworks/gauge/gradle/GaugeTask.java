/*
 *  Copyright 2015 Manu Sunny
 *
 *  This file is part of Gauge-gradle-plugin.
 *
 *  Gauge-gradle-plugin is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Gauge-gradle-plugin is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Gauge-gradle-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.thoughtworks.gauge.gradle;

import com.thoughtworks.gauge.gradle.exception.GaugeExecutionFailedException;
import com.thoughtworks.gauge.gradle.util.PropertyManager;
import com.thoughtworks.gauge.gradle.util.Util;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class GaugeTask extends DefaultTask {
    private final Logger log = LoggerFactory.getLogger("gauge");
    private GaugeExtension extension;

    private static final String TAGS_FLAG = "--tags";
    private static final String GAUGE = "gauge";
    private static final String PARALLEL_FLAG = "--parallel";
    private static final String DEFAULT_SPECS_DIR = "specs";
    private static final String NODES_FLAG = "-n";
    private static final String ENV_FLAG = "--env";
    private static final String GAUGE_CUSTOM_CLASSPATH_ENV = "gauge_custom_classpath";

    @TaskAction
    public void gauge() {
        Project project = getProject();
        extension = project.getExtensions().findByType(GaugeExtension.class);
        PropertyManager propertyManager = new PropertyManager(project, extension);
        propertyManager.setProperties();
        executeGaugeSpecs();
    }

    private void executeGaugeSpecs() throws GaugeExecutionFailedException {
        try {
            ProcessBuilder builder = createProcessBuilder();
            info("Executing command => " + builder.command());
            Process process = builder.start();
            Util.inheritIO(process.getInputStream(), System.out);
            Util.inheritIO(process.getErrorStream(), System.err);
            if (process.waitFor() != 0) {
                throw new GaugeExecutionFailedException("Execution failed for one or more tests!");
            }
        } catch (InterruptedException | NullPointerException e) {
            throw new GaugeExecutionFailedException(e);
        } catch (IOException e) {
            throw new GaugeExecutionFailedException("Gauge or Java runner is not installed! Read http://getgauge.io/documentation/user/current/getting_started/download_and_install.html");
        }
    }

    public ProcessBuilder createProcessBuilder() {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(createGaugeCommand());

        setClasspath(builder);
        return builder;
    }

    private void setClasspath(ProcessBuilder builder) {
        String classpath = extension.getClasspath();
        if (classpath == null) {
            classpath = "";
        }
        debug("Setting Custom classpath => %s", classpath);
        builder.environment().put(GAUGE_CUSTOM_CLASSPATH_ENV, classpath);
    }

    public ArrayList<String> createGaugeCommand() {
        ArrayList<String> command = new ArrayList<>();
        command.add(GAUGE);
        addTags(command);
        addParallelFlags(command);
        addEnv(command);
        addAdditionalFlags(command);
        addSpecsDir(command);
        return command;
    }

    private void addEnv(ArrayList<String> command) {
        String env = extension.getEnv();
        if (env != null && !env.isEmpty()) {
            command.add(ENV_FLAG);
            command.add(env);
        }
    }

    private void addAdditionalFlags(ArrayList<String> command) {
        String flags = extension.getAdditionalFlags();
        if (flags != null) {
            command.addAll(Arrays.asList(flags.split(" ")));
        }
    }

    private void addParallelFlags(ArrayList<String> command) {
        Boolean inParallel = extension.isInParallel();
        Integer nodes = extension.getNodes();
        if (inParallel != null && inParallel) {
            command.add(PARALLEL_FLAG);
            if (nodes != null && nodes != 0) {
                command.add(NODES_FLAG);
                command.add(Integer.toString(nodes));
            }
        }
    }

    private void addSpecsDir(ArrayList<String> command) {
        String specsDirectoryPath = extension.getSpecsDir();

        if (specsDirectoryPath != null) {
            validateSpecsDirectory(specsDirectoryPath);
            command.add(specsDirectoryPath);
        } else {
            warn("Property 'specsDir' not set. Using default value => '%s'", DEFAULT_SPECS_DIR);
            command.add(DEFAULT_SPECS_DIR);
        }
    }

    private void validateSpecsDirectory(String specsDirectoryPath) {
        File specsDirectory = new File(specsDirectoryPath);
        if (!specsDirectory.exists()) {
            error("Specs directory specified is not existing!");
            throw new GaugeExecutionFailedException("Specs directory specified is not existing!");
        }
    }

    private void addTags(ArrayList<String> command) {
        String tags = extension.getTags();
        if (tags != null && !tags.isEmpty()) {
            command.add(TAGS_FLAG);
            command.add(tags);
        }
    }

    private void warn(String format, String... args) {
        log.warn(String.format(format, args));
    }

    private void debug(String format, String... args) {
        log.debug(String.format(format, args));
    }

    private void error(String format, String... args) {
        log.error(String.format(format, args));
    }

    private void info(String format, String... args) {
        log.info(String.format(format, args));
    }
}