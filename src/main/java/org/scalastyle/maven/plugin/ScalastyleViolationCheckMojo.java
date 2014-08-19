// Copyright (C) 2011-2012 the original author or authors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.scalastyle.maven.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.scalastyle.Directory;
import org.scalastyle.FileSpec;
import org.scalastyle.Message;
import org.scalastyle.OutputResult;
import org.scalastyle.ScalastyleChecker;
import org.scalastyle.ScalastyleConfiguration;
import org.scalastyle.TextOutput;
import org.scalastyle.XmlOutput;

import scala.Option;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Entry point for scalastyle maven plugin.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class ScalastyleViolationCheckMojo extends AbstractMojo {

    /**
     * <p>
     * Specifies the location of the scalstyle XML configuration file to use.
     * </p>
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     * <p>
     * This parameter is resolved as file, classpath resource then URL.
     * </p>
     * <p>
     * <b>default_config.xml</b> from scalastyle classpath jar is used if non is specified in configuration
     * </p>
     * <p/>
     */
    @Parameter(property = "scalastyle.config.location", required = true, defaultValue = "default_config.xml")
    private String configLocation;

    /**
     * Specifies the path and filename to save the Scalastyle output.
     */
    @Parameter(property = "scalastyle.output.file")
    private File outputFile;

    /**
     * Specifies the encoding of the Scalastyle (XML) output
     */
    @Parameter(property = "scalastyle.output.encoding")
    private String outputEncoding;

    /**
     * Whether to fail the build if the validation check fails.
     */
    @Parameter(property = "scalastyle.failOnViolation", defaultValue = "true")
    private Boolean failOnViolation = Boolean.TRUE;

    /**
     * Specifies if the build should fail upon a warning level violation.
     */
    @Parameter(property = "scalastyle.failOnWarning", defaultValue = "false")
    private Boolean failOnWarning = Boolean.FALSE;

    /**
     * skip the entire goal
     */
    @Parameter(property = "scalastyle.skip", defaultValue = "false")
    private Boolean skip = Boolean.FALSE;

    /**
     * Print details of everything that Scalastyle is doing
     */
    @Parameter(property = "scalastyle.verbose", defaultValue = "false")
    private Boolean verbose = Boolean.FALSE;

    /**
     * Print very little.
     */
    @Parameter(property = "scalastyle.quiet", defaultValue = "false")
    private Boolean quiet = Boolean.FALSE;

    /**
     * Specifies the location of the Scala source directories to be used for
     * Scalastyle. This is only used if sourceDirectories is not specified
     */
    @Parameter
    private File sourceDirectory;

    /**
     * Specifies the location of the Scala source directories to be used for
     * Scalastyle.
     */
    @Parameter
    private File[] sourceDirectories;

    /**
     * Specifies the location of the Scala test source directories to be used
     * for Scalastyle. Only used if testSourceDirectories is not specified
     */
    @Parameter
    private File testSourceDirectory;

    /**
     * Specifies the location of the Scala test source directories to be used
     * for Scalastyle.
     */
    @Parameter
    private File[] testSourceDirectories;

    /**
     * Include or not the test source directory in the Scalastyle checks.
     */
    @Parameter(property = "scalastyle.includeTestSourceDirectory", defaultValue = "false")
    private Boolean includeTestSourceDirectory = Boolean.FALSE;

    /**
     * Directory containing the build files.
     */
    @Parameter(property = "scalastyle.build.directory", defaultValue = "${project.build.directory}")
    private File buildDirectory;

    /**
     * Base directory of the project.
     */
    @Parameter(property = "scalastyle.base.directory", defaultValue = "${basedir}")
    private File baseDirectory;

    /**
     * Specifies the encoding of the source files
     */
    @Parameter(property = "scalastyle.input.encoding")
    private String inputEncoding;


    /**
     * The Maven Project Object.
     */
    @Component
    protected MavenProject project;

    @Component
    private ResourceManager resourceManager;

    public void execute() throws MojoFailureException, MojoExecutionException {
        if (Boolean.TRUE.equals(skip)) {
            getLog().warn("Scalastyle:check is skipped as scalastyle.skip=true");
        } else {
            getLog().debug("failOnWarning=" + failOnWarning);
            getLog().debug("verbose=" + verbose);
            getLog().debug("quiet=" + quiet);
            for (File d : sourceDirectoriesAsList()) {
                getLog().debug("sourceDirectory=" + d);
            }
            for (File d : testSourceDirectoriesAsList()) {
                getLog().debug("testSourceDirectory=" + d);
            }

            getLog().debug("includeTestSourceDirectory=" + includeTestSourceDirectory);
            getLog().debug("buildDirectory=" + buildDirectory);
            getLog().debug("baseDirectory=" + baseDirectory);
            getLog().debug("outputFile=" + outputFile);
            getLog().debug("outputEncoding=" + outputEncoding);
            getLog().debug("inputEncoding=" + inputEncoding);

            performCheck();
        }
    }

    private void performCheck() throws MojoFailureException, MojoExecutionException {
        try {
            ScalastyleConfiguration configuration = ScalastyleConfiguration.readFromXml(getConfigFile(configLocation));
            long start = now();
            List<Message<FileSpec>> messages = new ScalastyleChecker<FileSpec>().checkFilesAsJava(configuration, getFilesToProcess());

            Config config = ConfigFactory.load(new ScalastyleChecker<FileSpec>().getClass().getClassLoader());
            OutputResult outputResult = new TextOutput<FileSpec>(config, verbose, quiet).output(messages);

            if (outputFile != null) {
                System.out.println("Saving to outputFile=" + outputFile.getAbsolutePath());
                saveToXml(config, outputFile, outputEncoding, messages);
            }

            if (!quiet) {
                System.out.println("Processed " + outputResult.files() + " file(s)");
                System.out.println("Found " + outputResult.errors() + " errors");
                System.out.println("Found " + outputResult.warnings() + " warnings");
                System.out.println("Found " + outputResult.infos() + " infos");
                System.out.println("Finished in " + (now() - start) + " ms");
            }

            int violations = outputResult.errors() + (Boolean.TRUE.equals(failOnWarning) ? outputResult.warnings() : 0);

            if (violations > 0) {
                if (failOnViolation) {
                    throw new MojoFailureException("You have " + violations + " Scalastyle violation(s).");
                } else {
                    getLog().warn("Scalastyle:check violations detected but failOnViolation set to " + failOnViolation);
                }
            } else {
                getLog().debug("Scalastyle:check no violations found");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed during scalastyle execution", e);
        }
    }

    private void saveToXml(Config config, File outputFile, String encodingString, List<Message<FileSpec>> messages) {
        String encoding = (encodingString != null) ? encodingString : System.getProperty("file.encoding");
        XmlOutput.save(config, outputFile.getAbsolutePath(), encoding, messages);
    }

    private long now() {
        return new Date().getTime();
    }

    private String getConfigFile(String configLocation) throws MojoFailureException {
        if (configLocation == null) {
            throw new MojoFailureException("configLocation is required");
        }

        if (new File(configLocation).exists()) {
            return configLocation;
        }

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClassLoaderWithProjectResources());
            File configFile = resourceManager.getResourceAsFile(configLocation);
            if ( configFile == null ){
                throw new MojoFailureException("Unable to process configuration file at location "+ configLocation);
            }
            return configFile.getAbsolutePath();
        } catch (ResourceNotFoundException e) {
            throw new MojoFailureException("Unable to find configuration file at location "+ configLocation);
        } catch (FileResourceCreationException e) {
            throw new MojoFailureException("Unable to process configuration file at location "+ configLocation,e);
        }finally {
            Thread.currentThread().setContextClassLoader(original);
        }

    }

    private URLClassLoader getClassLoaderWithProjectResources() throws MojoFailureException {
        List<String> classPathStrings = new ArrayList<String>();
        List<URL> urls = new ArrayList<URL>( classPathStrings.size() );

        try {
            classPathStrings.addAll(project.getTestCompileSourceRoots());
            classPathStrings.addAll(project.getCompileSourceRoots());

            for(Resource resource:project.getTestResources()){
                classPathStrings.add(resource.getDirectory());
            }
            for(Resource resource:project.getResources()){
                classPathStrings.add(resource.getDirectory());
            }
            for ( String path : classPathStrings ){
                urls.add(new File(path).toURI().toURL());
            }
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(),e);
        }

        return new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader());
    }

    private List<FileSpec> getFilesToProcess() {
        List<FileSpec> all = new ArrayList<FileSpec>();

        all.addAll(getFiles("sourceDirectory", sourceDirectoriesAsList(), inputEncoding));
        all.addAll(getFiles("testSourceDirectory", testSourceDirectoriesAsList(), inputEncoding));

        return all;
    }

    private List<FileSpec> getFiles(String name, List<File> dirs, String encoding) {
        List<FileSpec> files = new ArrayList<FileSpec>();

        for (File dir : dirs) {
            if (isDirectory(dir)) {
                getLog().debug("processing " + name + "=" + dir + " encoding=" + encoding);
                files.addAll(Directory.getFilesAsJava(Option.apply(encoding), Collections.singletonList(dir)));
            } else {
                getLog().warn(name + " is not specified or does not exist value=" + dir);
            }
        }

        return files;
    }

    private boolean isDirectory(File file) {
        return file != null && file.exists() && file.isDirectory();
    }

    private List<File> sourceDirectoriesAsList() {
        return arrayOrValue(sourceDirectories, sourceDirectory);
    }

    private List<File> testSourceDirectoriesAsList() {
        return (!includeTestSourceDirectory) ? new LinkedList<File>() : arrayOrValue(testSourceDirectories, testSourceDirectory);
    }

    private List<File> arrayOrValue(File[] array, File value) {
        return (array != null) ? Arrays.asList(array) : Collections.singletonList(value);
    }
}

