/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.performance.simulation

import io.gatling.core.Predef._
import io.gatling.core.structure.{PopulatedScenarioBuilder, ScenarioBuilder}
import uk.gov.hmrc.performance.conf.{HttpConfiguration, JourneyConfiguration, PerftestConfiguration}
import uk.gov.hmrc.performance.feeder.CsvFeeder

import scala.util.Random


trait PerformanceTestRunner extends Simulation
with HttpConfiguration
with JourneyConfiguration
with PerftestConfiguration {

  private[simulation] val parts = scala.collection.mutable.MutableList[JourneyPart]()

  def setup(id: String, description: String): JourneyPart = {

    val part: JourneyPart = new JourneyPart(id, description)
    parts += part
    part
  }

  private def journeys: Seq[Journey] = {

    println(s"Implemented journey parts: ${parts.map(_.id).mkString(", ")}")

    definitions.map(conf => {

      println(s"Setting up scenario '${conf.id}' to run at ${conf.load} JPS")

      val partsInJourney = conf.parts.map(p => parts.find(_.id.trim == p.trim)
        .getOrElse(throw new IllegalArgumentException(s"Scenario '${conf.id}' is configured to run '$p' but there is no journey part for it in the code"))
      )

      val first = group(partsInJourney.head.description) {
        partsInJourney.head.builder
      }
      val chain = partsInJourney.tail.foldLeft(first)((c, p) => c.group(p.description) {
        p.builder
      })

      new Journey {

        lazy val feeder = conf.feeder
        val RNG = new Random

        override lazy val builder: ScenarioBuilder = {
          val scenarioBuilder = scenario(conf.description)
          if (!feeder.isEmpty) scenarioBuilder.feed(new CsvFeeder(feeder))
          scenarioBuilder
            .feed(Iterator.continually(Map("currentTime" -> System.currentTimeMillis().toString)))
            .feed(Iterator.continually(Map("random" -> Math.abs(RNG.nextInt()))))
            .exitBlockOnFail(exec(chain))
        }

        override lazy val load: Double = conf.load
      }

    })
  }

  private def withAtLeasOneRequestInTheFullTest(load: Double) = load match {
    case rate if (constantRateTime.toSeconds * rate).toInt < 1 => 1D / (constantRateTime.toSeconds - 1)
    case rate => rate
  }

  private def withInjectedLoad(journeys: Seq[Journey]): Seq[PopulatedScenarioBuilder] = journeys.map(scenario => {

    val load = withAtLeasOneRequestInTheFullTest(scenario.load * loadPercentage)

    val injectionSteps = List(
      rampUsersPerSec(noLoad).to(load).during(rampUpTime),
      constantUsersPerSec(load).during(constantRateTime),
      rampUsersPerSec(load).to(noLoad).during(rampDownTime)
    )

    scenario.builder.inject(injectionSteps)
  })

  def runSimulation(): Unit = {


    println(s"Setting up simulation ")

    if (runSingleUserJourney) {

      println(s"'perfetest.runSmokeTest' is set to true, ignoring all loads and running with only one user per journey!")

      val injectedBuilders = journeys.map(scenario => {
        scenario.builder.inject(atOnceUsers(1))
      })

      setUp(injectedBuilders: _*)
        .protocols(httpProtocol)
        .assertions(global.failedRequests.count.is(0))
    } else {
      setUp(withInjectedLoad(journeys): _*)
        .protocols(httpProtocol)
        .assertions(global.failedRequests.percent.lessThan(1))
    }
  }
}
