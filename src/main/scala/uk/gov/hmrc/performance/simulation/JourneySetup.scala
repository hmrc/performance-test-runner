/*
 * Copyright 2023 HM Revenue & Customs
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

import io.gatling.core.Predef.{constantUsersPerSec, exec, group, rampUsersPerSec, scenario, _}
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.core.structure.{PopulationBuilder, ScenarioBuilder}
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.hmrc.performance.conf.{JourneyConfiguration, PerftestConfiguration}
import uk.gov.hmrc.performance.feeder.CsvFeeder

import scala.util.Random

/** A trait to setup the Journeys used in Simulation.
  */

trait JourneySetup extends JourneyConfiguration with PerftestConfiguration {

  private val logger: Logger = LoggerFactory.getLogger(classOf[JourneySetup])

  private[simulation] val parts = scala.collection.mutable.ListBuffer[JourneyPart]()

  /** Creates JourneyPart which allows to chain HttpRequestBuilder and ActionBuilder.
    *
    * @param id Unique identifier for the setup. This should match an entry in the parts provided in journeys.conf
    * @param description description of the setup. This value is used in Gatling's scenario description which is
    *                    surfaced in gatling report.
    * @return JourneyPart
    */
  def setup(id: String, description: String): JourneyPart = {

    val part: JourneyPart = JourneyPart(id, description)
    parts += part
    part
  }

  /**
    * Builds uk.gov.hmrc.performance.simulation.Journey for each journey defined in journeys.conf
    * @return a Seq of uk.gov.hmrc.performance.simulation.Journey for all the journeys defined in journeys.conf
    */
  protected def journeys: Seq[Journey] = {

    logger.info(s"Implemented journey parts: ${parts.map(_.id).mkString(", ")}")

    definitions(labels).map { conf =>
      logger.info(s"Setting up scenario '${conf.id}' to run at ${conf.load} JPS and load to $loadPercentage %")

      val partsInJourney = conf.parts.map(p =>
        parts
          .find(_.id.trim == p.trim)
          .getOrElse(
            throw new IllegalArgumentException(
              s"Scenario '${conf.id}' is configured to run '$p' but there is no journey part for it in the code"
            )
          )
      )

      val first = group(partsInJourney.head.description) {
        partsInJourney.head.builder
      }
      val chain = partsInJourney.tail.foldLeft(first)((c, p) =>
        c.group(p.description) {
          p.builder
        }
      )

      new Journey {

        lazy val feeder: String = conf.feeder
        val RNG                 = new Random

        override lazy val builder: ScenarioBuilder = {
          val scenarioBuilder =
            if (feeder.nonEmpty) scenario(conf.description).feed(new CsvFeeder(feeder))
            else scenario(conf.description)
          scenarioBuilder
            .feed(Iterator.continually(Map("currentTime" -> System.currentTimeMillis().toString)))
            .feed(Iterator.continually(Map("random" -> Math.abs(RNG.nextInt()))))
            .exitBlockOnFail(exec(chain))
        }

        override lazy val load: Double = conf.load
      }

    }
  }

  private def withAtLeasOneRequestInTheFullTest(load: Double) = load match {
    case rate if (constantRateTime.toSeconds * rate).toInt < 1 => 1d / (constantRateTime.toSeconds - 1)
    case rate                                                  => rate
  }

  /** Calculates the load for the provided journeys and constructs the injection steps using
    * Gatling's open injection model: https://gatling.io/docs/current/general/simulation_setup/#open-model.
    *
    * @param journeys Seq of journeys as defined in journeys.conf
    * @return Sequence of PopulationBuilder with calculated injection step
    *         injected into io.gatling.core.structure.ScenarioBuilder
    */
  protected def withInjectedLoad(journeys: Seq[Journey]): Seq[PopulationBuilder] = journeys.map { scenario =>
    val load = withAtLeasOneRequestInTheFullTest(scenario.load * loadPercentage)

    val injectionSteps: List[OpenInjectionStep] = List(
      rampUsersPerSec(noLoad).to(load).during(rampUpTime),
      constantUsersPerSec(load).during(constantRateTime),
      rampUsersPerSec(load).to(noLoad).during(rampDownTime)
    )

    scenario.builder.inject(injectionSteps)
  }

}
