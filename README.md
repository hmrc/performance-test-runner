
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

### Implement your first simulation

##### Step 1: Implement the requests to your pages

```scala
import uk.gov.hmrc.performance.conf.ServicesConfiguration

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object HelloWorldRequests extends ServicesConfiguration {

  val baseUrl = baseUrl("hello-world-frontend")

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

##### Step 2: Setup the simulation

```scala
import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import HelloWorldRequests._

class HelloWorldSimulation extends PerformanceTestRunner {

  setup("login", "Login") withRequests (navigateToLoginPage, submitLogin)

  setup("home", "Go to the homepage") withRequests navigateToHome

  runSimulation()
}
```

##### Conditionally run a setup step:
The `toRunIf` method can be used to conditionally run a setup step based on a value in the Gatling session. 
If the value in the Gatling session for the provided sessionKey matches the expected value then the setup 
is executed.

In the below example, the `post-vat-return-period` setup will be run only if the Gatling session has a value 200 for the sessionKey `check-status`.

```scala
setup("post-vat-return-period", "Post vat return period") withRequests postVatReturnPeriod toRunIf("${check-status}", "200")
```

**Note:**
When the `toRunIf` condition is not met, then all requests within the setup will not be executed.

##### Step 3. Configure the journeys 

journeys.conf

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
    run-if = ["label-B"]
  }

}
```

##### Step 4. Configure the tests

application.conf

```
runLocal = true

baseUrl = "http://helloworld-service.co.uk"

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
  percentageFailureThreshold = 5

}

}
```

##### Step 4. Configure the user feeder

helloworld.csv

```csv
username,password
bob,12345678
alice,87654321
```

In your csv you can use placeholders:<br>
`${random}` is replaced with a random int value<br>
`${currentTime}` is replaced with the current time in milliseconds<br>
`${range-X}` is replaced by a string representation of number made of X digits. 
The number is incremental and starts from 1 again when it reaches the max value. 
For example ${range-3} will be replaced with '001' the first time, '002' the next and so on. 

This allows you to create a csv with only one record but an infinite number of random values 
```csv
username,password
my-${random}-user,12345678
```

##### Creating a custom user feeder:
The csv user feeder described above should be sufficient for most data driven tests. When csv feeder is not sufficient, custom feeders can be 
created. 

_Example:_

To create a feeder that generates random UUID, create an Iterator[Map[Key, Value]] and pass it to Gatling's feed method.

```scala
val randomUUIDs: Iterator[Map[String, String]] = Iterator.continually(Map("uuid" -> UUID.randomUUID().toString))
def uuidFeeder: ChainBuilder = feed(randomUUIDs)
```

To use this feeder, chain the setup by passing the feeder to performance-test-runner's `withActions`

```scala
setup("home-page", "Home Page") withActions(uuidFeeder.actionBuilders:_*) withRequests navigateToHomePage
```

To use the value from the feeder within a request, use the feeder's key which is `uuid` in our example

```
"searchCriterion": "${uuid}"
```

### Run a smoke test

To run a smoke test through all journeys, with one user only, set the following.

```
sbt -Dperftest.runSmokeTest=true -Djava.io.tmpdir=${WORKSPACE}/tmp test
```

### Run the test

To run the full performance test execute

```
sbt -Djava.io.tmpdir=${WORKSPACE}/tmp test
```


### More about setting up the simulation 
#### Using a pause 

```scala
import io.gatling.core.action.builder.PauseBuilder
import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import HelloWorldRequests._

import scala.concurrent.duration._

class HelloWorldSimulation extends PerformanceTestRunner {
  
  val pause = new PauseBuilder(1 milliseconds, None)
  
  setup("login", "Login") withActions(navigateToLoginPage, pause, submitLogin)

  runSimulation()
}
```

#### Using Gatling's Session API 

Gatling's [Session API](https://gatling.io/docs/3.4/session/session_api/) is used to update
Gatling's Session. To use the Session API with performance-test-runner, the `ChainBuilder` returned when executing a Session
API should be converted into an `ActionBuilder`. The `ActionBuilder` then can be chained to a `setup` using `withActions`. 

An example:

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

### More about the journey configuration.

`description` will be assigned to the journey in the test report

`load` is the number of journeys that will be started every second

`feeder` is the relative path to the csv feeder file. More [here](http://gatling.io/docs/2.1.7/session/feeder.html#csv-feeders)

`parts` is the list of parts that combined create your journey

`run-if` is an optional list of labels. Runs this journey only if a label from this list is passed in the `labels` parameter of application.conf

`skip-if` is an optional list of labels. Skips this journey if a label from this list is passed in the `labels` parameter of application.conf

You can have as many journeys as you like in journeys.conf, the simulation will start and run them all together.


### More about the services configuration.

Contains the name of the service and the port when running locally. Read the services-local.conf file for more details and examples.


### More about application.conf

`runLocal` boolean value to run test locally. Default value is true.

`baseUrl` is the default url for every service. Read the application.conf file for more details and examples.

`rampupTime` is the time in minutes for the simulation to inject the users with a linear ramp up

`constantRateTime` is the time in minutes for the simulation to inject users at a constant rate

`rampdownTime` is the time in minutes for the simulation to inject the users with a linear ramp down

`loadPercentage` is the percentage of the load for the journeys. Read the application.conf file for more details and examples.

`journeysToRun` contains the journeys that will be executed. Leave it empty if you want to run all the journeys

`labels` optional string containing a comma-separated list of test labels. Read the application.conf file for more details.

`percentageFailureThreshold` optional int. Read the application.conf file for more details.

## Scalafmt
This repository uses [Scalafmt](https://scalameta.org/scalafmt/), a code formatter for Scala. The formatting rules configured for this repository are defined within [.scalafmt.conf](.scalafmt.conf).

To apply formatting to this repository using the configured rules in [.scalafmt.conf](.scalafmt.conf) execute:

 ```
 sbt scalafmtAll scalafmtSbt
 ```

To check files have been formatted as expected execute:

 ```
 sbt scalafmtCheckAll scalafmtSbtCheck
 ```

[Visit the official Scalafmt documentation to view a complete list of tasks which can be run.](https://scalameta.org/scalafmt/docs/installation.html#task-keys)


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    
