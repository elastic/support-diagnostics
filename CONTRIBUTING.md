# How to contribute

This is a tool used by the Elastic Support team to collect the necessary data to troubleshoot problems. For new features or enhancements, open a github issue to discuss before submitting a pull request.

## Getting Started

### Using IntelliJ IDEA

- Clone the project
- In IntelliJ, use `New -> Project from Existing Sources...`
- Select `Import project from external model` and use the `Maven` option
- Use the default options for the Project
- You will need to add the following to the POM file
  ```xml
  <orderEntry type="module-library" scope="RUNTIME">
    <library>
      <CLASSES>
        <root url="file://$MODULE_DIR$/src/main/resources" />
      </CLASSES>
      <JAVADOC />
      <SOURCES />
    </library>
  </orderEntry>
  ```
- Using the `Run` menu and select `Edit Configurations...`. Use the `+` to add an `Application` configuration.
  - Main class: `co.elastic.support.diagnostics.DiagnosticApp`
  - Program arguments (example): `-o ~/tmp/diag-output -h localhost -u elastic --ptp changeme`. Put whatever arguments you would like to run the application with as default.

### Releasing to Maven Central

In order to release the code to Maven Central, you must have a Sonatype account
with the permissions to deploy to the `co.elastic` `groupId`.

Once created, you will need to create or modify your Maven `settings.xml`
(`~/.m2/settings.xml` is for global usage). Example:

```xml
<settings>
  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>your_xml_encoded_passphrase</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
  <servers>
    <server>
      <id>ossrh</id>
      <username>your_sonatype_username</username>
      <password>your_xml_encoded_sonatype_password</password>
    </server>
  </servers>
</settings>
```

Note: `ossrh` matches the `id` used in the `pom.xml`. You can use any version of
`gpg` that you want.

Once the `settings.xml` is setup, you can run

```
mvn clean deploy
```

This will deploy based on the version in the `pom.xml` file (`-SNAPSHOT` creates
it in their snapshot repository, which you should always do before a real
release).

More detailed instructions can be found on
[Sonatype's website](https://central.sonatype.org/publish/publish-maven/).
