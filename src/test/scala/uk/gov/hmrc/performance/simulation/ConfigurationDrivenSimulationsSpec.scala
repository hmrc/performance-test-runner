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

import io.gatling.core.config.GatlingConfiguration
import uk.gov.hmrc.play.test.UnitSpec

class ConfigurationDrivenSimulationsSpec extends UnitSpec {

  import io.gatling.core.Predef._
  import io.gatling.http.Predef._

  GatlingConfiguration.setUpForTest()

  class TestSimulation extends ConfigurationDrivenSimulations {

    val foo = http("Get Foo").get(s"/foo")
    val bar = http("Get Bar").get(s"/bar")

    setup("some-id-1", "Some Description 1") withRequests (foo, bar)

    setup("some-id-2", "Some Description 2") withRequests bar

    override def runSimulation(): Unit = {}
  }


  class MalformedTestSimulation extends ConfigurationDrivenSimulations {

    setup("some-id-1", "Some Description 1")

    override def runSimulation(): Unit = {}
  }



  "The simulation" should {
    "create some parts" in {

      val simulation: TestSimulation = new TestSimulation()

      simulation.parts.size shouldBe 2

      simulation.parts.head.id shouldBe "some-id-1"
      simulation.parts.head.description shouldBe "Some Description 1"
      simulation.parts.head.builder.actionBuilders.size shouldBe 2

      simulation.parts(1).id shouldBe "some-id-2"
      simulation.parts(1).description shouldBe "Some Description 2"
      simulation.parts(1).builder.actionBuilders.size shouldBe 1

    }

    "Throw an exception if the journey part has no requests" in {
      val thrown = intercept[IllegalArgumentException] {
        new MalformedTestSimulation().parts.head.builder
      }
      thrown.getMessage shouldBe "'some-id-1' must have at least one request"
    }
  }
}
