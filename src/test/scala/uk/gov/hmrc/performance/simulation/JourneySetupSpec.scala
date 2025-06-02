/*
 * Copyright 2024 HM Revenue & Customs
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

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.http.request.builder.HttpRequestBuilder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class JourneySetupSpec extends AnyWordSpec with Matchers {

  import io.gatling.core.Predef._
  import io.gatling.http.Predef._

  class TestRequestsSetup extends JourneySetup {
    val foo: HttpRequestBuilder = http("Get Foo").get(s"/foo")
    val bar: HttpRequestBuilder = http("Get Bar").get(s"/bar")

    setup("some-id-1", "Some Description 1").withRequests(foo, bar)
    setup("some-id-2", "Some Description 2").withRequests(bar)
  }

  class TestActionsSetup extends JourneySetup {
    val fooBuilder: HttpRequestBuilder = http("Get Foo").get(s"/foo")
    val pauseBuilder: ActionBuilder    = pause(Duration(1, MILLISECONDS)).actionBuilders.head
    setup("some-id-1", "Some Description 1").withActions(fooBuilder, pauseBuilder)
  }

  class MalformedTestSetup extends JourneySetup {
    setup("some-id-1", "Some Description 1")
  }

  "The simulation" should {
    "create some parts from http requests" in {
      val simulation: TestRequestsSetup = new TestRequestsSetup()

      simulation.parts.size shouldBe 2

      simulation.parts.head.id                          shouldBe "some-id-1"
      simulation.parts.head.description                 shouldBe "Some Description 1"
      simulation.parts.head.builder.actionBuilders.size shouldBe 2

      simulation.parts(1).id                          shouldBe "some-id-2"
      simulation.parts(1).description                 shouldBe "Some Description 2"
      simulation.parts(1).builder.actionBuilders.size shouldBe 1

    }

    "create some parts from general actions" in {
      val simulation: TestActionsSetup = new TestActionsSetup()

      simulation.parts.size shouldBe 1

      simulation.parts.head.id                          shouldBe "some-id-1"
      simulation.parts.head.description                 shouldBe "Some Description 1"
      simulation.parts.head.builder.actionBuilders.size shouldBe 2
    }

    "Throw an exception if the journey part has no requests" in {
      val thrown = intercept[IllegalArgumentException] {
        new MalformedTestSetup().parts.head.builder
      }
      thrown.getMessage shouldBe "'some-id-1' must have at least one request"
    }
  }
}
