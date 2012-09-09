## maven-scalastyle-plugin
Welcome to maven-scalastyle-plugin.
This project is intended to provide maven plugin support for Scalastyle.
For more information about Scalastyle, see [https://github.com/scalastyle/scalastyle](https://github.com/scalastyle/scalastyle)

## Goals Overview
* **scalastyle:check** performs a violation check against the scalastyle config file to see if there are any violations. 
It counts the number of violations found and displays it on the console if verbose is enabled.

## Usage
### Check scalacheck violation part of build cycle
To Configure the Scalastyle Plugin, you need to the add it in the <build> section of your pom.xml as shown in the sample below,
and by default the build will fail if there are any violations of level error found.
Default phase of execution is `verify`. The following is an example of a configuration which would be used in a pom:
 
      <build>
         <plugins> 
          ...
		  <plugin>
			<groupId>org.scalastyle</groupId>
			<artifactId>scalastyle-maven-plugin</artifactId>
			<version>0.1.0</version>
			<configuration>
			  <verbose>false</verbose>
			  <failOnViolation>true</failOnViolation>
			  <includeTestSourceDirectory>true</includeTestSourceDirectory>
			  <failOnWarning>false</failOnWarning>
			  <sourceDirectory>${basedir}/src/main/scala</sourceDirectory>
			  <testSourceDirectory>${basedir}/src/test/scala</testSourceDirectory>
			  <configLocation>${basedir}/lib/scalastyle_config.xml</configLocation>
			</configuration>
			<executions>
			  <execution>
				<goals>
				  <goal>check</goal>
				</goals>
			  </execution>
			</executions>
		  </plugin>
            ...
        </plugins>
    </build>
