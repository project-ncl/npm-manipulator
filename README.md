# npm-manipulator


NPM Manipulator is a tool used to manipulate NPM project definition files. Various manipulations can be performed on a project and their execution can be controlled by provided arguments.

# Usage

```
usage: ...
 -t,--type                The project type. Can be only NPM for now and is not mandatory.
                          It is not case-sensitive.
 -f,--file <arg>          Project definition file
 -r,--result <arg>        Json file to be generated at the end of manipulation containing
                          the results. Is not mandatory.
 -D <arg>                 Java properties
 -h,--help                Print help
 -l,--log <arg>           Log file to output logging to
    --log-context <arg>   Add log-context ID
 -d,--debug               Enable debug
 -c,--trace               Enable trace
```
e.g.
```
java -jar npm-manipulator-cli.jar -f npm-project/package.json
```

# Java Properties

You can specify java system properties in the CLI by using `-D<prop>=<value>`

| Java Property | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| --- |-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| restURL | Dependency Analysis REST URL (e.g http://da.url.com/da/rest/v-1). It is used and required when user wants to perform automatic version increment.                                                                                                                                                                                                                                                                                                                                         |
| restConnectionTimeout | Optional connection timeout to set for the underlying HTTP client library responsible for calling the REST endpoints. Defaults to 30 seconds.                                                                                                                                                                                                                                                                                                                                             |
| restSocketTimeout | Optional socket timeout to set for the underlying HTTP client library responsible for calling the REST endpoints. Defaults to 10 minutes.                                                                                                                                                                                                                                                                                                                                                 |
| restMode | Mode indicating which versions (temporary versions, managed service versions etc) from Dependency Analysis.                                                                                                                                                                                                                                                                                                                                                                               |
| packageScope | A package scope that should be added or changed to.                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| versioningStrategy | Versioning strategy can be either HYPHENED or SEMVER. The former uses hyphens between the original version, requested suffix and auto-incremented number, e.g. "1.2.3-jboss-001". The latter auto-increments the patch number to first available number and does not require suffix. If defined, it will also append it and add a build number separated by a dot resulting in SemVer pre-release format, e.g. "1.2.0-rc.1". It is mandatory when requesting automatic version increment. |
| versionIncrementalSuffix | The version suffix to append to version of current project. It is used when automatic version increment is performed. It is mandatory with HYPHENED versioning strategy and optional with SEMVER one.                                                                                                                                                                                                                                                                                     |
| versionIncrementalSuffixPadding | With automatic version increment, it is possible to configure zero-padding for the incremented number in the suffix. This is used only with HYPHENED versioning strategy. Default: 1                                                                                                                                                                                                                                                                                                      |
| versionOverride | Desired version used as the output of version manipulation overriding all the logic. If set, the manipulation only updates project version to this and does not do anything else (no communication with Dependency Analysis, no suffix computation etc).                                                                                                                                                                                                                                  |
| versionBaseOverride | Replacement version to be used as the base in place of the original project version before running the logic computing the suffix.                                                                                                                                                                                                                                                                                                                                                        |
| versionSuffixOverride | Desired version suffix, that will be appended to the current version. It overrides the logic computing the suffix number automatically.                                                                                                                                                                                                                                                                                                                                                   |
| manipulation.disable | default: false, specify whether you want to disable the manipulation of the version or not                                                                                                                                                                                                                                                                                                                                                                                                |
| dependencyOverride.$package_name | Desired version(s) to apply to the specified package(s), if listed inside the _dependencies_ in package.json. Does not replace the values in lock files. Example: `-DdependencyOverride.keycloak-admin-client=^0.12.0 -DdependencyOverride.async=1.5.2`                                                                                                                                                                                                                                   |
| devDependencyOverride.$package_name | Desired version(s) to apply to the specified package(s), if listed inside the _devDependencies_ in package.json. Does not replace the values in lock files. Example: `-DdevDependencyOverride.keycloak-admin-client=^0.12.0`                                                                                                                                                                                                                                                              |
| preScript | Run a shell script before manipulation. Accepts a comma separated list of file:// or http:// URLs.                                                                                                                                                                                                                                                                                                                                                                                        |
| postScript | Run a shell script after manipulation. Accepts a comma separated list of file:// or http:// URLs.                                                                                                                                                                                                                                                                                                                                                                                                                   |

Note: If this tool is running in the context of a [PNC Reqour](https://github.com/project-ncl/reqour/) environment any changes made via the shell script pre/post functionality will be committed back to SCM.

### OpenTelemetry Instrumentation

If `OTEL_EXPORTER_OTLP_ENDPOINT` is defined (and optionally `OTEL_SERVICE_NAME`) then OpenTelemetry instrumentation
will be activated. It will read trace information from the environment as described [here](https://github.com/jenkinsci/opentelemetry-plugin/blob/master/docs/job-traces.md#environment-variables-for-trace-context-propagation-and-integrations) and will propagate the information via headers in any REST calls.

# Notes

The project is inspired by and partially based on
[POM Manipulation Extension](https://github.com/release-engineering/pom-manipulation-ext)
