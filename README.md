
# configuration-driven-simulation

[![Build Status](https://travis-ci.org/hmrc/configuration-driven-simulation.svg?branch=master)](https://travis-ci.org/hmrc/configuration-driven-simulation) [ ![Download](https://api.bintray.com/packages/hmrc/releases/configuration-driven-simulation/images/download.svg) ](https://bintray.com/hmrc/releases/configuration-driven-simulation/_latestVersion)


This is a wrapper around the [Gatling](http://gatling.io/) load testing framework, 
with pre configured injection steps, protocols and assertions.


### Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "http-verbs" % "x.x.x"
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

##### Step 3. Configure the journeys. 

```
journeys {

  hello-world = {
    description = "Hello world journey"
    load = 9.1
    feeder = data/helloworld.csv
    parts = [
      login,
      home
    ]
  }

}
```

##### Step 4. Configure the user feeder

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

TODO

### More about application.conf

TODO

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    
