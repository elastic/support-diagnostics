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
  - Program arguments (example): `-o ~/tmp/diag-output -h localhost -u elastic --passwordText changeme`. Put whatever arguments you would like to run the application with as default.

### Running Buildkite CI on a fork pull request

Buildkite does not automatically build pull requests opened from forks, to
avoid running untrusted code with CI credentials. If you are a maintainer and
want to run the Buildkite pipeline against a fork PR, comment
`buildkite test this <commit-sha>` on the PR, naming the exact commit to build
(a full or GitHub-UI-abbreviated SHA, at least 7 hex characters). This is
handled by
[`.github/workflows/buildkite-pr-command.yml`](.github/workflows/buildkite-pr-command.yml),
which only triggers a build for commenters with write/maintain/admin access to
this repository, and refuses to build if the hash is missing, malformed, does
not match the PR's current head commit, or names a commit created after the
comment.

Fork PRs naturally must be checked before running builds.

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
./gradlew publish
```

This will deploy based on the version in the `gradle.properties` file (`-SNAPSHOT` creates
it in their snapshot repository, which you should always do before a real
release).

More detailed instructions can be found on
[Sonatype's website](https://central.sonatype.org/publish/publish-maven/).

Once deployed, you must release the library through
[Sonatype's staging repository](https://oss.sonatype.org/#stagingRepositories),
using your Sonatype credentials. You first "Close" the staged deployment, then
"Release" it after it passes the validations from "Close".
