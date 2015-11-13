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

package uk.gov.hmrc.performance.conf

import com.typesafe.config.ConfigFactory
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Properties

class JourneyConfigurationSpec extends UnitSpec {

  /*
    hello-world = {
      description = "Hello world journey"
      load = 9.1
      feeder = data/helloworld.csv
      parts = [
        login,
        helloworld-home
      ]
    }
   */

  "JourneyConfiguration" should {

    "be able to read a well formed journey.conf file" in {
      val configUnderTest = new JourneyConfiguration {}

      configUnderTest.definitions should contain(JourneyDefinition(
        id = "hello-world-1",
        description = "Hello world journey 1",
        load = 9.1,
        parts = List("login", "helloworld-home"),
        feeder = "data/helloworld.csv"
      ))
    }

    "be able to return only journeys set in the application.config file" in {

      Properties.setProp("journeysToRun.0", "hello-world-1")
      Properties.setProp("journeysToRun.1", "hello-world-3")
      ConfigFactory.invalidateCaches()
      val configUnderTest = new JourneyConfiguration {}

      configUnderTest.definitions should contain theSameElementsAs Seq(JourneyDefinition(
        id = "hello-world-1",
        description = "Hello world journey 1",
        load = 9.1,
        parts = List("login", "helloworld-home"),
        feeder = "data/helloworld.csv"
      ),
        JourneyDefinition(
          id = "hello-world-3",
          description = "Hello world journey 3",
          load = 3.3,
          parts = List("helloworld-home"),
          feeder = "data/helloworld.csv"
        ))
    }

  }

}
