## scalastyle-maven-plugin 
Welcome to the scalastyle-maven-plugin wiki!
This project is intended to provide maven plugin support  for Scalastyle.
For more information about Scalastyle, see [https://github.com/scalastyle/scalastyle](https://github.com/scalastyle/scalastyle)

## Goals Overview
* **scalastyle:check** performs a violation check against the scalastyle config file to see if there are any violations.It counts the number of violations found and displays it on the console if verbose is enabled.

## Usage
### Check scalacheck violation part of build cycle
To Configure the Scalastyle Plugin, you need to the add it in the <build> section of your pom.xml as shown in the sample below. Default phase of execution is `process-sources` and build will fail if there any voilation
 
      <build>
         <plugins> 
          ...
            <plugin>
                <groupId>org.sss.scala-tools</groupId>
                <artifactId>scalastyle-maven-plugin</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <configuration>
                    <verbose>true</verbose>
                    <failOnViolation>true</failOnViolation>
                    <includeTestSourceDirectory>true</includeTestSourceDirectory>
                    <failsOnWarning>true</failsOnWarning>
                    <configLocation>${basedir}/scalastyle_config.xml</configLocation>
                </configuration>
            </plugin>
           ...
        </plugins>
    </build>
