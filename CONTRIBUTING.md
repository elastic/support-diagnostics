# How to contribute

This is a tool used by the Elastic Support team to collect the necessary data to troubleshoot problems. For new features or enhancements, open a github issue to discuss before submitting a pull request.


## Getting Started

### Using IntelliJ IDEA
* Clone the project
* In IntelliJ, use `New -> Project from Existing Sources...`
* Select `Import project from external model` and use the `Maven` option
* Use the default options for the Project
* You will need to add the following to the POM file
    ``` xml
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
* Using the `Run` menu and select `Edit Configurations...`. Use the `+` to add an `Application` configuration.
  * Main class: `co.elastic.support.diagnostics.DiagnosticApp`
  * Program arguments (example): `-o ~/tmp/diag-output -h localhost -u elastic --ptp changeme`. Put whatever arguments you would like to run the application with as default.
