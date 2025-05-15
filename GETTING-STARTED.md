## Implementing your first simulation

### Table of Contents
* [Prerequisite](#prerequisite)
* [Adding a new journey](#adding-a-new-journey)
* [Defining the journey parts](#defining-the-journey-parts)
* [Implementing the HTTP requests](#implementing-the-http-requests)
* [Configuring the tests](#configuring-the-tests)
* [Running the tests](#running-the-tests)
* [Assertions](#assertions)
* [Feeder files to inject test data](#feeder-files-to-inject-test-data)
* [Using Gatling's Exec method with performance-test-runner](#using-gatlings-exec-method-with-performance-test-runner)
* [Using a pause builder between requests](#using-a-pause-builder-between-requests)
* [Using Gatling's Session API](#using-gatlings-session-api)
* [Repeat a request](#repeat-a-request)
* [Iterate asLongAs](#iterate-asLongAs)
* [Conditionally run a setup step](#conditionally-run-a-setup-step)

### Prerequisite
A performance test repository has already been created using create-a-test-repository Jenkins job or using the
[performance-testing-template.g8](https://github.com/hmrc/performance-testing-template.g8).

### Adding a new Journey
The performance test journeys are defined in a file named `journeys.conf` following [HOCON](https://github.com/lightbend/config#using-hocon-the-json-superset).
One or more journeys are wrapped inside a `journeys` block. Each `journey` is a key value pair, where the key is as a
unique`journey id` and the value is made up of the following properties.

`description` - A mandatory field providing the journey description. This is assigned to the journey in the test report.

`load` - A mandatory field defining the number of journeys that will be started every second.

`feeder` - An optional field containing the relative path to the csv feeder file. More about using feeders
[here](#feeder-files-to-inject-test-data).

`parts` - A mandatory field including all parts that make a journey.

`run-if` - An optional list of labels. Runs this journey only if a label from this list is passed in the `labels` parameter
of `application.conf`.

`skip-if`- An optional list of labels. Skips this journey if a label from this list is passed in the `labels` parameter
of `application.conf`.

```
journeys {
  hello-world-1 = {
    description = "Hello world journey 1"
    load = 9.1
    feeder = data/helloworld.csv
    parts = [
      login,
      home
    ]
    run-if = ["label-A"]
  }
  
  hello-world-2 = {
    description = "Hello world journey 2"
    load = 6.2
    feeder = data/helloworld.csv
    parts = [
      login
    ]
    skip-if = ["label-B"]
  }
}
```

### Defining the journey parts
The parts specified in a journey are defined within a class that extends
[PerformanceTestRunner](src/main/scala/uk/gov/hmrc/performance/simulation/PerformanceTestRunner.scala). Each setup
is made up of one or more requests. The setup can also include any additional actions like
[pause builder](#using-a-pause-builder-between-requests), [custom feeder](#creating-a-custom-user-feeder),
[modifying the Gatling session](#using-gatlings-session-api), or [conditionally run a setup step](#conditionally-run-a-setup-step).

```scala
import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import HelloWorldRequests._

class HelloWorldSimulation extends PerformanceTestRunner {

  setup("login", "Login") withRequests (navigateToLoginPage, submitLogin)

  setup("home", "Go to the homepage") withRequests navigateToHome

  runSimulation()
}
```

### Implementing the HTTP requests
The individual requests included in the setup are implemented with Gatling's [HTTP DSL](https://gatling.io/docs/current/http/http_request/).

```scala
import uk.gov.hmrc.performance.conf.ServicesConfiguration

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object HelloWorldRequests extends ServicesConfiguration {

  /**
   * baseUrlFor: A utility in performance-test-runner's ServicesConfiguration 
   * to construct base URLs from services.conf or services-local.conf
   */
  val baseUrl = baseUrlFor("hello-world-frontend")

  def navigateToLoginPage =
    http("Navigate to Login Page")
      .get(s"$baseUrl/login")
      .check(status.is(200))


  def submitLogin = {
    http("Submit username and password")
      .post(s"$baseUrl/login": String)
      .formParam("userId", "${username}")
      .formParam("password", "${password}")
      .check(status.is(303))
      .check(header("Location").is("/home"))
  }

  def navigateToHome =
    http("Navigate To Home")
      .get(s"$baseUrl/home")
      .check(status.is(200))
}
```

### Configuring the tests
The library parses a variety of configuration files to configure a performance test.

#### application.conf
This file contains the configuration for load, duration of the test, and the percentage failure threshold. For more details
take a look at the default `application.conf` created by
[performance-testing-template.g8](https://github.com/hmrc/performance-testing-template.g8/blob/master/src/main/g8/src/test/resources/application.conf)

Most of the properties in `application.conf` is commented out by default. This is because:
* Most of these properties have default values set in [PerfTestConfiguration](src/main/scala/uk/gov/hmrc/performance/conf/PerftestConfiguration.scala)
* Some of these properties are overridden when running tests in Jenkins.

```
{
runLocal = true

perftest {
  rampupTime = 1
  constantRateTime = 5 
  rampdownTime = 1
  loadPercentage = 100
  journeysToRun = [
    hello-world-1,
    hello-world-2
  ],
  labels = "label-A, label-B"   #optional
  percentageFailureThreshold = 1
  requestPercentageFailureThreshold = 1 
  }
}
```

#### services.conf
Contains the http configuration of the individual services. This configuration is of the format:

```
services {
    example-frontend {
    protocol = https
    host = www.staging.tax.service.gov.uk
    port = 443
    }
}
```

#### services-local.conf
To run tests against locally running services, provide the configuration within services-local.conf. This will be of the format:

```
services {
    example-frontend {
    protocol = http
    host = localhost
    port = 9080 //port where the service is running locally
    }
}
```

#### gatling.conf
Contains [Gatling specific configuration](https://gatling.io/docs/current/general/configuration/#gatling-conf).
This requires updating only when any of the default values in the config needs to be changed.

#### logback.xml
To increase the logging level, set Gatling logs to `DEBUG` or `TRACE`. This configuration is already included in `logback.xml`
, but commented out by default.

```
<!-- set to DEBUG to log all failing HTTP requests -->
<!-- set to TRACE to log all HTTP requests -->
<logger name="io.gatling.http.engine.response" level="TRACE" />
```

**Note:** *Do not run a full performance test with `DEBUG` or `TRACE` level in Jenkins as this can result in an 
out-of-memory error due to the size of the console output.*

### Running the tests

#### Running a smoke test
To run a smoke test through all journeys, with one user only, use the following command.
```
sbt -Dperftest.runSmokeTest=true -DrunLocal=false gatling:test
```

#### Running a smoke test locally
To run a smoke test through all journeys, with one user only, set `runLocal=true`. Setting this property will use the service
configuration defined in [services-local.conf](#services-localconf)
```
sbt -Dperftest.runSmokeTest=true -DrunLocal=true gatling:test
```

#### Running a full performance test
A full performance test should be executed only from the Performance Jenkins instance. To run the full performance test:
```
sbt -DrunLocal=false gatling:test
```
### Assertions
The performance-test-runner library is [pre-configured](src/main/scala/uk/gov/hmrc/performance/conf/PerftestConfiguration.scala) with the following assertions:
* Overall failed requests count should be less than 1% of all the requests
* Individual failed requests count should be less than 1% of individual requests

The failure % is configurable within the [application.conf](#applicationconf) of the test repository.

**Note:** *The default assertions provided are there for guidance only. It is important that teams review each test run 
in order to ensure the results meet their requirements.*

### Feeder files to inject test data
Feeder files can be used to inject test data specific to each user. CSV feeder is the only type of feeder supported by default.
Refer to [Creating a custom user feeder](#creating-a-custom-user-feeder) section to create additional custom feeders.

#### Configuring the CSV feeder
The contents of the `CSV` can be configured using placeholders.

```csv
username,password
bob,12345678
alice,87654321
```
For example, the above `CSV` file can be configured with a `random` placeholder as below. During test execution, the `random`
placeholder will be replaced by a random number for every user. 
```csv
username,password
my-${random}-user,12345678
```
The available placeholders are:<br>
`${random}` is replaced with a random int value<br>
`${currentTime}` is replaced with the current time in milliseconds<br>
`${range-X}` is replaced by a string representation of number made of X digits. The number is incremental and starts 
from 1 again when it reaches the max value. For example ${range-3} will be replaced with '001' the first time, '002' 
the next and so on.

### Using Gatling's Exec method with performance-test-runner
Gatling's Exec method is used to execute an action. Actions are usually requests like an HTTP request. Additionally, actions can edit or debug a Gatling session.

For example, the contents of a Session can be displayed using the Gatling DSL as below:
```scala
exec { session =>
  // displays the content of the session in the console (debugging only)
  println(session)

  // return the original session
  session
}
```
You can read more about the Exec function in the [Gatling documentation](https://gatling.io/docs/gatling/reference/3.4/general/scenario/#exec)

The `exec` function returns a ChainBuilder. To use `exec` with performance-test-runner, convert the ChainBuilder to an ActionBuilder. 
So, to display the contents of the Gatling session with performance-test-runner:

```scala
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Session
import io.gatling.core.Predef._

/** Executes the Session API to print session information.
 * Converts the resulting ChainBuilder to a List[ActionBuilder] to use with `withActions`
 */
val printSession: List[ActionBuilder] =
  exec { session =>
    println(session)
    session
  }.actionBuilders

// Prints the session information after navigating to the home page
setup("home-page", "Home Page") withRequests navigateToHomePage withActions(printSession: _*)
```

Below, you can find ways to use some common Gatling functions with performance-test-runner.

#### Creating a custom user feeder

> [!NOTE]
> Before you create a custom feeder, check if Gatling already provides a suitable random generator function for your use case.
>
> Gatling 3.9.0 introduced the following random generators: 
>- randomUUID, 
>- randomSecureUuid,
>- randomAlphanumeric,
>- randomInt,
>- randomLong,
>- randomDouble,

The `CSV` user feeder described above should be sufficient for most data driven tests. When `CSV` feeder is not sufficient, custom feeders can be
created.

For example, to create a feeder that generates random UUID, create an Iterator[Map[Key, Value]] and pass it to Gatling's feed method:

```scala
val randomUUIDs: Iterator[Map[String, String]] = Iterator.continually(Map("uuid" -> UUID.randomUUID().toString))
def uuidFeeder: ChainBuilder = feed(randomUUIDs)
```

To use this feeder, chain the setup by passing the feeder to performance-test-runner's `withActions`

```scala
setup("home-page", "Home Page") withActions(uuidFeeder.actionBuilders:_*) withRequests navigateToHomePage
```

The value then can be extracted using Gatling's [Expression Language](https://gatling.io/docs/3.4/session/expression_el/). 

```
 def submitUniqueId = {
    http("Submit unique id")
      .post(s"$baseUrl/some-endpoint": String)
      .formParam("uniqueId", "${uuid}")
  }
```

#### Using a pause builder between requests

```scala
import io.gatling.core.action.builder.PauseBuilder
import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import HelloWorldRequests._

import scala.concurrent.duration._
// required to convert FiniteDuration to session.Expression[FiniteDuration] to use in PauseBuilder
import io.gatling.core.Predef._  

class HelloWorldSimulation extends PerformanceTestRunner {
  
  val pause = new PauseBuilder(1 milliseconds, None)
  
  setup("login", "Login") withActions(navigateToLoginPage, pause, submitLogin)

  runSimulation()
}
```

#### Using Gatling's Session API
Gatling's [Session API](https://gatling.io/docs/3.4/session/session_api/) is used to update Gatling's Session. To use
the Session API with performance-test-runner, the `ChainBuilder` returned when executing a Session API should be 
converted into an `ActionBuilder`. The `ActionBuilder` then can be chained to a `setup` using `withActions`.

```scala
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Session
import io.gatling.core.Predef._

/** Executes the Session API to set testId with value perf-12345 in Gatling's Session.
 *  Converts the resulting ChainBuilder to a List[ActionBuilder] 
 */
val setRandomTestId: List[ActionBuilder] = {
    exec((session: Session) => session.set("testId", "perf-12345}"))
  }.actionBuilders

// Chains the setRandomTestId using `withActions`
setup("prep", "Prepare for test") withActions (setRandomTestId:_*)
```

#### Repeat a request
A request or requests can be repeated multiple times using Gatling's [repeat](https://gatling.io/docs/gatling/reference/3.4/general/scenario/#repeat) function.
This is useful when you want to repeat certain requests for a certain number of time. For example, adding 3 items during a journey.

```scala
def repeatRequests: List[ActionBuilder] = {
  repeat(3) {
    exec(addItem)
      .exec(getItemPage)
  }.actionBuilders
}

setup("post-vat-return-period", "Post vat return period")  withRequests(addItem) withActions (repeatRequests:_*)
```

#### Iterate asLongAs
Use `asLongAs` to iterate [as long as](https://gatling.io/docs/gatling/reference/3.4/general/scenario/#aslongas) the 
`condition` is satisfied. `condition` is a session function that returns a boolean value. 

For example, to repeat a request `asLongAs` the status of a page is **not** `200`

```scala
//Import required to use `asLongAs` and to implicitly convert boolean to  session.Expression[Boolean] 
import io.gatling.core.Predef._

def getTurnoverPage: List[ActionBuilder] = {
  asLongAs(session => 
    !session.attributes.get("turnOverPageStatus").contains(200)) {
    exec(http("Get Turnover Page")
      .get(s"$baseUrl$${turnOverPage}": String)
      .check(status.saveAs("turnOverPageStatus")))
  }.actionBuilders
}
```
**NOTE:** `asLongAs` also takes additional optional parameters `counterName` and `exitASAP`.

> When session key is set externally, for example in an earlier request, ensure the session keys are reset where required.
> See the [session API section](#using-gatlings-session-api) for updating Gatling's session. 


Checkout Gatling's [Scenario documentation](https://gatling.io/docs/gatling/reference/3.4/general/scenario/) for all
available functions.

### Conditionally run a setup step
The `toRunIf` method can be used to conditionally run a setup step based on a value in the Gatling session. If the value
in the Gatling session for the provided sessionKey matches the expected value then the setup is executed.

In the below example, the `post-vat-return-period` setup will be run only if the Gatling session has a value 200
for the sessionKey `check-status`.

```scala
setup("post-vat-return-period", "Post vat return period") withRequests postVatReturnPeriod toRunIf("${check-status}", "200")
```

**Note:**
When the `toRunIf` condition is not met, then all requests within the setup will not be executed.