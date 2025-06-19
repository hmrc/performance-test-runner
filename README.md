
# performance-test-runner

This is a wrapper around the [Gatling](http://gatling.io/) load testing framework,
with preconfigured injection steps, protocols and assertions.

### Adding to your performance test

Add the below library to your Scala dependencies (e.g. `Dependencies.scala`):
```
"uk.gov.hmrc" %% "performance-test-runner" % "6.1.0" % Test
```

Add the below plugin to your `plugins.sbt`:
```
addSbtPlugin("io.gatling" % "gatling-sbt" % "4.9.2")
```

Your tests will need both the `performance-test-runner` library AND the `gatling-sbt` plugin. This library is available 
for Scala 2.13 only.

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


### Upgrade notes

#### `v6.0.0`
If you are upgrading to `v6.0.0` of `performance-test-runner`, you can now remove dependencies that previously needed 
adding manually.

If you are using `v6.0.0` of `performance-test-runner`, you can remove the following from your dependencies
(e.g. `Dependencies.scala`) as these are now added by the library:

```sbt
"io.gatling"            % "gatling-test-framework"    % "x.x.x" % Test,
"io.gatling.highcharts" % "gatling-charts-highcharts" % "x.x.x" % Test,
"com.typesafe"          % "config"                    % "x.x.x" % Test
```

### v6.2.0
If you are upgrading to `v6.2.0` of `performance-test-runner`, you will need to ensure your `gatling-sbt` plugin 
version is greater or equal to `4.2.6`. 

You will also need to ensure you are using Java 11 or greater and if you're using `slf4j` in your project, 
make sure it is greater than 2.0.0.

If you are using CheckBuilder in your performance tests, you would previously have passed in three parameters:
```scala
    CheckBuilder[HttpStatusCheckType, Response, Int]
```
CheckBuilder now only expects two parameters:
```scala
    CheckBuilder[HttpStatusCheckType, Response]
```

#### New Gatling Expression Language Features
Gatling Expression Language (EL) has introduced new random generator functions:
- randomUUID()
- randomSecureUuid()
- randomAlphanumeric()
- randomInt()
- randomLong()
- randomDouble()

These are provided out of the box, you can use them like this in a Gatling expression:
```
#{randomInt()}
```

FeederBuilderBase<T> now has access to a `recordsCount` method, so you do not need to do any extra work in Scala to
get the length of the records.

### Gatling Expression Language syntax change from ${} to #{}
If you use the Gatling Expression Language, due to the clashes with Scala string interpolation, Gatling has been updated to utilise `#{}` as the format for defining expressions.

#### Pebble Extensions
As part of Gatling 3.9, Pebble has been upgraded to version 3.2.0. If you are using custom pebble extensions, you will need to change the import package from `com.mitchellbosecke` to `io.pebbletemplates`.

### Scala 3 Considerations
If you're using the scala 3 build of performance-test-runner, you may need to wrap your `Expression[T]` strings in `StaticValueExpression`, e.g `StaticValueExpression("your-string"). You will need to import this from `gatling.core.session`:

```scala
import io.gatling.core.session.StaticValueExpression
```

#### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
