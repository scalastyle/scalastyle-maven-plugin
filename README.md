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

```xml
    <build>
        <plugins> 
          ...
          <plugin>
            <groupId>org.scalastyle</groupId>
            <artifactId>scalastyle-maven-plugin</artifactId>
            <version>0.8.0</version>
            <configuration>
              <verbose>false</verbose>
              <failOnViolation>true</failOnViolation>
              <includeTestSourceDirectory>true</includeTestSourceDirectory>
              <failOnWarning>false</failOnWarning>
              <sourceDirectory>${project.basedir}/src/main/scala</sourceDirectory>
              <testSourceDirectory>${project.basedir}/src/test/scala</testSourceDirectory>
              <configLocation>${project.basedir}/lib/scalastyle_config.xml</configLocation>
              <outputFile>${project.basedir}/scalastyle-output.xml</outputFile>
              <outputEncoding>UTF-8</outputEncoding>
              <exclusions>
                <exclusion>.*directory.*</exclusion>
              </exclusions>
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
```

You can also specify multiple source directories if necessary. Replace the <sourceDirectory> element with <sourceDirectories>:

```xml
    <sourceDirectories>
      <dir>${project.basedir}/src/main/scala</dir>
      <dir>${project.basedir}/src/main/generated-scala</dir>
    </sourceDirectories>
```

and similarly for `testSourceDirectory` & `testSourceDirectories`.

You can also specify multiple exclusion filters in form of regex if necessary. Add the <exclusion> element in <exclusions>:

```xml
    <exclusions>
      <exclusion>.*testdir1.*</exclusion>
      <exclusion>.*testdir2.*</exclusion>
    </exclusions>
```

Only files that match **none** of the provided `exclusions` are processed.
