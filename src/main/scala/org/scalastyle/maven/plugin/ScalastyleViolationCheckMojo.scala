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

package org.scalastyle.maven.plugin

import java.io.File
import collection.mutable.ListBuffer
import org.scalastyle._
import org.apache.maven.plugin.{MojoExecutionException, MojoFailureException, AbstractMojo}
import org.scala_tools.maven.mojo.annotations._
import java.util.Date

/**
 * Entry point for scalastyle maven plugin.
 */
@description("Integrates Scalastyle style checker for Scala with maven")
@goal("check")
@phase("verify")
@requiresProject
class ScalastyleViolationCheckMojo extends AbstractMojo {
  /**
   * Specifies the configuration file location
   *
   * @parameter expression="${scalastyle.config.location}"
   * @required
   */
  @parameter
  @expression("${scalastyle.config.location}")
  @required
  var configLocation: String = _

  /**
   * Specifies the path and filename to save the Scalastyle output.
   * @parameter expression="${scalastyle.output.file}"
   *            default-value="${project.build.directory}/scalastyle-result.xml"
   */
  @parameter
  @expression("${scalastyle.output.file}")
  var outputFile: File = _

  /**
   * Whether to fail the build if the validation check fails.
   *
   * @parameter expression="${scalastyle.failOnViolation}"
   *            default-value="true"
   */
  @parameter
  @expression("${scalastyle.failOnViolation}")
  var failOnViolation: java.lang.Boolean = true

  /**
   * Specifies if the build should fail upon a warning level violation.
   *
   * @parameter expression="${scalastyle.failOnWarning}" default-value="false"
   */
  @parameter
  @expression("${scalastyle.failOnWarning}")
  var failOnWarning: java.lang.Boolean = false

  /**
   * skip the entire goal
   *
   * @parameter expression="${scalastyle.skip}" default-value="false"
   */
  @parameter
  @expression("${scalastyle.skip}")
  var skip: java.lang.Boolean = false

  /**
   * Print details of everything that Scalastyle is doing
   *
   * @parameter expression="${scalastyle.verbose}" default-value="false"
   */
  @parameter
  @expression("${scalastyle.verbose}")
  var verbose: java.lang.Boolean = false

  /**
   * Print very little.
   *
   * @parameter expression="${scalastyle.quiet}" default-value="false"
   */
  @parameter
  @expression("${scalastyle.quiet}")
  var quiet: java.lang.Boolean = false

  /**
   * Specifies the location of the scala source directory to be used for Scalastyle.
   *
   * @parameter default-value="${project.build.sourceDirectory}"
   * @required
   */
  @parameter
  @expression("${project.build.sourceDirectory}")
  @required
  var sourceDirectory: File = _

  /**
   * Specifies the location of the scala test source directory to be used for Scalastyle.
   *
   * @parameter default-value="${project.build.testSourceDirectory}"
   */
  @parameter
  @expression("${project.build.testSourceDirectory}")
  @required
  var testSourceDirectory: File = _

  /**
   * Include or not the test source directory in the Scalastyle checks.
   *
   * @parameter default-value="${false}"
   */
  @parameter
  @expression("${scalastyle.includeTestSourceDirectory}")
  var includeTestSourceDirectory: java.lang.Boolean = false

  /**
   * Directory containing the build files.
   *
   * @parameter expression="${project.build.directory}"
   */
  @parameter
  @expression("${project.build.directory}")
  @required
  var buildDirectory: File = _

  /**
   * Base directory of the project.
   *
   * @parameter expression="${basedir}"
   */
  @parameter
  @expression("${basedir}")
  @required
  var baseDirectory: File = _

  @throws(classOf[MojoExecutionException])
  @throws(classOf[MojoFailureException])
  def execute() {
    if (skip) {
      getLog.debug("Scalastyle:check is skipped as it is turned on")
    } else {
      getLog.debug("failOnWarning=" + failOnWarning)
      getLog.debug("verbose=" + verbose)
      getLog.debug("quiet=" + quiet)
      getLog.debug("sourceDirectory=" + sourceDirectory)
      getLog.debug("testSourceDirectory=" + testSourceDirectory.getAbsolutePath())
      getLog.debug("includeTestSourceDirectory=" + includeTestSourceDirectory)
      getLog.debug("buildDirectory=" + buildDirectory)
      getLog.debug("baseDirectory=" + baseDirectory)
      performCheck()
    }
  }

  private[this] def performCheck() {
    checkConfigFile(configLocation)
    try {
      val configuration = ScalastyleConfiguration.readFromXml(configLocation)
      val start = now()
      val messages = new ScalastyleChecker[FileSpec].checkFiles(configuration, getFilesToProcess)

      val outputResult = new TextOutput[FileSpec](verbose, quiet).output(messages)

      // scalastyle:off regex
      if (outputFile != null) {
        println("Saving to outputFile=" + outputFile.getAbsolutePath());
        XmlOutput.save(outputFile, messages)
      }
      if (!quiet) println("Processed " + outputResult.files + " file(s)")
      if (!quiet) println("Found " + outputResult.errors + " errors")
      if (!quiet) println("Found " + outputResult.warnings + " warnings")
      if (!quiet) println("Finished in " + (now - start) + " ms")
      // scalastyle:on regex

      val violations = outputResult.errors + (if (failOnWarning) outputResult.warnings else 0)

      if (violations > 0) {
        if (failOnViolation) {
          throw new MojoFailureException("You have " + violations + " Scalastyle violation(s).")
        } else {
          getLog.warn("Scalastyle:check violations detected but failOnViolation set to " + failOnViolation)
        }
      } else {
        getLog.debug("Scalastyle:check no violations found")
      }
    } catch {
      case e: Exception => throw new MojoExecutionException("Failed during scalastyle execution", e);
    }
  }

  private[this] def now(): Long = new Date().getTime()

  private[this] def checkConfigFile(configLocation: String): Unit = {
    if (configLocation == null) {
      throw new MojoFailureException("configLocation is required")
    }

    if (!new File(configLocation).exists()) {
      throw new MojoFailureException("configLocation " + configLocation + " does not exist")
    }
  }

  private[this] def getFilesToProcess: List[FileSpec] = {
    val sd = getFiles("sourceDirectory", sourceDirectory)
    val tsd = if (includeTestSourceDirectory) getFiles("testSourceDirectory", testSourceDirectory) else Nil

    sd ::: tsd
  }

  private[this] def getFiles(name: String, file: File) = {
    if (isDirectory(file)) {
      getLog.debug("processing " + name + "=" + file)
      Directory.getFiles(file)
    } else {
      getLog.warn(name + " is not specified or does not exist value=" + file)
      Nil
    }
  }

  private[this] def isDirectory(file: File) = file != null && file.exists() && file.isDirectory()
}
