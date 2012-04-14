package org.sss.scalatools.scalastyle.plugin

import java.io.File
import collection.mutable.ListBuffer
import org.scalastyle._
import org.apache.maven.plugin.{MojoExecutionException, MojoFailureException, AbstractMojo}
import org.scala_tools.maven.mojo.annotations._

/**
 * Entry point for scalastyle maven plugin.
 */
@description("Integrates Scalastyle style checker for Scala with maven")
@goal("check")
@phase("process-sources")
@requiresProject
class ScalastyleViolationCheckMojo extends AbstractMojo {

  /**
   * Specifies the configuration file location
   *
   * @parameter expression="${scalastyle.config.file}"
   * @required
   */
  @parameter
  @expression("${scalastyle.config.file}")
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
   * @parameter expression="${scalastyle.failOnViolation}"  default-value="true"
   */
  @parameter
  @expression("${scalastyle.failOnViolation}")
  var failOnViolation: Boolean = true

  /**
   * Specifies if the build should fail upon a warning level violation.
   *
   * @parameter expression="${scalastyle.failsOnWarning}" default-value="false"
   */
  @parameter
  @expression("${scalastyle.failsOnWarning}")
  var failsOnWarning: Boolean = false

  /**
   * skip the entire goal
   *
   * @parameter expression="${scalastyle.skip}" default-value="false"
   */
  @parameter
  @expression("${scalastyle.skip}")
  var skip: Boolean = false

  /**
   * Whether to build an aggregated report at the root, or build individual reports.
   *
   * @parameter expression="${aggregate}" default-value="false"
   */
  @parameter
  @expression("${aggregate}")
  var aggregate: Boolean = false

  /**
   * Print details of check failures to build output.
   *
   * @parameter expression="${scalastyle.verbose}" default-value="false"
   */
  @parameter
  @expression("${scalastyle.verbose}")
  var verbose: Boolean = false


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
   * Include or not the test source directory to be used for Scalastyle.
   *
   * @parameter default-value="${false}"
   */
  @parameter
  @expression("${scalastyle.includeTestSourceDirectory}")
  var includeTestSourceDirectory: Boolean = false

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
      return;
    }

    val configuration = ScalastyleConfiguration.readFromXml(getConfigFile)
    try {
      val messages = new ScalastyleChecker[FileSpec].checkFiles(configuration, getFilesToProcess)

      val outputResult = new TextOutput[FileSpec](verbose, !verbose).output(messages)

      if (verbose) println("Processed " + outputResult.files + " file(s)")
      if (verbose) println("Found " + outputResult.errors + " errors")
      if (verbose) println("Found " + outputResult.warnings + " warnings")

      val violations = outputResult.errors + (if (failsOnWarning) outputResult.warnings else 0)

      if (failOnViolation && violations > 0) {
        val msg = "You have " + violations + " Scalastyle violation" + (if (violations > 1) "s" else "") + ".";
        throw new MojoFailureException(msg);
      }
      if (violations > 0)
        getLog.warn("Scalastyle:check violations detected but failOnViolation set to " + failOnViolation);

    } catch {
      case e: Exception => throw new MojoExecutionException("Failed during scalastyle execution", e);
    }

  }

  def getConfigFile: String = {
    if (configLocation == null)
      throw new MojoFailureException("configLocation is required");
    if (!new File(configLocation).exists())
      throw new MojoFailureException("configLocation " + configLocation + " does not exists");

    configLocation;
  }


  def getFilesToProcess: List[FileSpec] = {
    val specs = ListBuffer[FileSpec]()

    if (sourceDirectory != null && sourceDirectory.exists() && testSourceDirectory.isDirectory()) {
      specs.appendAll(Directory.getFiles(sourceDirectory));
    }

    if (includeTestSourceDirectory && testSourceDirectory != null
      && testSourceDirectory.exists() && testSourceDirectory.isDirectory()) {
      specs.appendAll(Directory.getFiles(testSourceDirectory));
    }
    specs.toList
  }

}
