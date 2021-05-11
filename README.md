
# performance-test-runner

This is a wrapper around the [Gatling](http://gatling.io/) load testing framework, 
with preconfigured injection steps, protocols and assertions.

### Adding to your performance test

#### Compatible versions

Library Version | Scala Version | gatling-version*          | gatling-sbt plugin
--------------- | ------------- | ------------------------- | ------------------ 
4.x.x           | 2.12          | 3.4.2                     | 3.2.1
3.x.x           | 2.11, 2.12    | 2.2.5, 2.3.1              | 2.2.0, 2.2.2

Gatling version refers to the version of the below Gatling dependencies:
- gatling-test-framework
- gatling-charts-highcharts

Add the below dependencies:

```scala
"uk.gov.hmrc"          %% "performance-test-runner"   % "x.x.x" % Test,
"io.gatling"            % "gatling-test-framework"    % "x.x.x" % Test,
"io.gatling.highcharts" % "gatling-charts-highcharts" % "x.x.x" % Test
```

Add the below plugin:
```
addSbtPlugin("io.gatling" % "gatling-sbt" % "x.x.x")
```
### Getting started

Refer to the [getting-started](GETTING-STARTED.md) guide for implementing your first simulation.

### Development

#### Scalafmt

This repository uses [Scalafmt](https://scalameta.org/scalafmt/), a code formatter for Scala. The formatting rules
configured for this repository are defined within [.scalafmt.conf](.scalafmt.conf).

To apply formatting to this repository using the configured rules in [.scalafmt.conf](.scalafmt.conf) execute:

 ```
 sbt scalafmtAll scalafmtSbt
 ```

To check files have been formatted as expected execute:

 ```
 sbt scalafmtCheckAll scalafmtSbtCheck
 ```

[Visit the official Scalafmt documentation to view a complete list of tasks which can be run.](https://scalameta.org/scalafmt/docs/installation.html#task-keys)


#### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").