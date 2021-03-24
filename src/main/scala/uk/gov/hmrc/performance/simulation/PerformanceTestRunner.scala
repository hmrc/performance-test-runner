/*
 * Copyright 2021 HM Revenue & Customs
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
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.hmrc.performance.conf.HttpConfiguration

/** Trait extending `io.gatling.core.scenario.Simulation`. Use within a performance test to set up Journeys and invoke
  *  `runSimulation()`, a method to configure the Simulation setup.
  */

trait PerformanceTestRunner extends Simulation with HttpConfiguration with JourneySetup {

  private val logger: Logger = LoggerFactory.getLogger(classOf[PerformanceTestRunner])

  /** Configures `io.gatling.core.scenario.Simulation.setUp`. This method is invoked from within a performance test.
    *
    * For smoke tests i.e when `uk.gov.hmrc.performance.conf.PerftestConfiguration.runSingleUserJourney`` is `true`,
    * the setUp is configured to run only for 1 user.
    *
    * For a full test, the setUp is configured with:
    *
    *  - Journeys and the load to run
    *  - Duration of the run
    *  - Protocol configuration and
    *  - Assertions
    */
  def runSimulation(): Unit = {

    import scala.concurrent.duration._
    val timeoutAtEndOfTest: FiniteDuration = 5 minutes

    logger.info(s"Setting up simulation ")

    if (runSingleUserJourney) {

      logger.info(
        s"'perftest.runSmokeTest' is set to true, ignoring all loads and running with only one user per journey!"
      )

      val injectedBuilders = journeys.map { scenario =>
        scenario.builder.inject(atOnceUsers(1))
      }

      setUp(injectedBuilders: _*)
        .protocols(httpProtocol)
        .assertions(global.failedRequests.count.is(0))
    } else {
      setUp(withInjectedLoad(journeys): _*)
        .maxDuration(rampUpTime + constantRateTime + rampDownTime + timeoutAtEndOfTest)
        .protocols(httpProtocol)
        .assertions(global.failedRequests.percent.lte(percentageFailureThreshold))
        .assertions(forAll.failedRequests.percent.lte(requestPercentageFailureThreshold))
    }
  }
}
