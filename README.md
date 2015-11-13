
# configuration-driven-simulation

[![Build Status](https://travis-ci.org/hmrc/configuration-driven-simulation.svg?branch=master)](https://travis-ci.org/hmrc/configuration-driven-simulation) [ ![Download](https://api.bintray.com/packages/hmrc/releases/configuration-driven-simulation/images/download.svg) ](https://bintray.com/hmrc/releases/configuration-driven-simulation/_latestVersion)


This is a wrapper around the [Gatling](http://gatling.io/) load testing framework, 
with pre configured injection steps, protocols and assertions.


### Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "configuration-driven-simulation" % "x.x.x"
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
import uk.gov.hmrc.performance.simulation.ConfigurationDrivenSimulations
import HelloWorldRequests._

class HelloWorldSimulation extends ConfigurationDrivenSimulations {

  setup("login", "Login") withRequests (navigateToLoginPage, submitLogin)

  setup("home", "Go to the homepage") withRequests navigateToHome

  runSimulation()
}
```

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
  }
  
  hello-world-2 = {
    description = "Hello world journey 2"
    load = 6.2
    feeder = data/helloworld.csv
    parts = [
      login
    ]
  }

}
```

##### Step 4. Configure the tests

application.conf

```
runLocal = true

base-url = "http://helloworld-service.co.uk"

perftest {
  rampup_time = 1
  constant_rate_time = 5
  rampdown_time = 1
  load_percentage = 100
  journeys_to_run = [
    hello-world-1,
    hello-world-2
  ]
}

}
```

##### Step 4. Configure the user feeder

helloworld.csv

```csv
username,password
bob,12345678
```

### Run the test locally

To run the performance test execute

```
sbt test
```

### More about the journey configuration.

`description` will be assigned to the journey in the test report

`load` is the number of journeys that will be started every second

`feeder` is the relative path to the csv feeder file. More [here](http://gatling.io/docs/2.1.7/session/feeder.html#csv-feeders)

`parts` is the list of parts that combined create your journey

You can have as many journeys as you like in journeys.conf, the simulation will start and run them all together.

### More about the services configuration.

Contains the name of the service and the port when running locally. Read the services-local.conf file for more details and examples.

### More about application.conf

`runLocal` boolean value to run test locally. Default value is true.

`base-url` is the default url for every service. Read the application.conf file for more details and examples.

`rampup_time` is the time in minutes for the simulation to inject the users with a linear ramp up

`constant_rate_time` is the time in minutes for the simulation to inject users at a constant rate

`rampdown_time` is the time in minutes for the simulation to inject the users with a linear ramp down

`load_percentage` is the percentage of the load for the journeys. Read the application.conf file for more details and examples.

`journeys_to_run` contains the journeys that will be executed. Leave it empty if you want to run all the journeys

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    
