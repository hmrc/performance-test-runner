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

package uk.gov.hmrc.performance.conf

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._
import scala.util.Properties

class JourneyConfigurationSpec extends AnyWordSpec with Matchers {

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

      configUnderTest.definitions() should contain(
        JourneyDefinition(
          id = "hello-world-1",
          description = "Hello world journey 1",
          load = 9.1,
          parts = List("login", "home"),
          feeder = "data/helloworld.csv",
          runIf = Set.empty
        )
      )
    }

    "filter the definitions using test labels set if this is not empty" in {
      val configUnderTest = new JourneyConfiguration {}

      configUnderTest.definitions(Set("label-A")) shouldBe Seq(
        JourneyDefinition(
          id = "hello-world-1",
          description = "Hello world journey 1",
          load = 9.1,
          parts = List("login", "home"),
          feeder = "data/helloworld.csv",
          runIf = Set.empty
        )
      )
    }

    "be able to return only journeys set in the application.config file" in {

      Properties.setProp("journeysToRun.0", "hello-world-1")
      Properties.setProp("journeysToRun.1", "hello-world-3")
      ConfigFactory.invalidateCaches()
      val configUnderTest = new JourneyConfiguration {}

      configUnderTest.definitions() should contain theSameElementsAs Seq(
        JourneyDefinition(
          id = "hello-world-1",
          description = "Hello world journey 1",
          load = 9.1,
          parts = List("login", "home"),
          feeder = "data/helloworld.csv",
          runIf = Set.empty
        )
      )

      Properties.clearProp("journeysToRun.0")
      Properties.clearProp("journeysToRun.1")
      ConfigFactory.invalidateCaches()
    }

    "use the feeder value configured in journey definition" in {
      val configUnderTest = new JourneyConfiguration {
        override lazy val applicationConfig: Config = ConfigFactory
          .load("journeys")
          .withFallback(
            ConfigFactory.parseMap(
              Map[String, Any](
                "journeys.journey-with-feeder.description" -> "Journey with feeder",
                "journeys.journey-with-feeder.load"        -> "0.1",
                "journeys.journey-with-feeder.parts"       -> List("home", "login").asJava,
                "journeys.journey-with-feeder.feeder"      -> "data/example.csv"
              ).asJava
            )
          )
      }
      val actualJourney   = configUnderTest.definitions().filter(_.id == "journey-with-feeder")
      actualJourney               should have size 1
      actualJourney.head.feeder shouldBe "data/example.csv"
    }

    "have a empty feeder when feeder is not configured in journey definition " in {
      val configUnderTest = new JourneyConfiguration {
        override lazy val applicationConfig: Config = ConfigFactory
          .load("journeys")
          .withFallback(
            ConfigFactory.parseMap(
              Map[String, Any](
                "journeys.journey-without-feeder.description" -> "Journey without feeder",
                "journeys.journey-without-feeder.load"        -> "0.1",
                "journeys.journey-without-feeder.parts"       -> List("home", "login").asJava
              ).asJava
            )
          )
      }
      val actualJourney   = configUnderTest.definitions().filter(_.id == "journey-without-feeder")
      actualJourney               should have size 1
      actualJourney.head.feeder shouldBe empty
    }

    "include run-if labels in description when specified in journey definition" in {
      val configUnderTest = new JourneyConfiguration {}
      val actualJourney   = configUnderTest.definitions(Set("label-B")).filter(_.id == "hello-world-3")
      actualJourney                    should have size 1
      actualJourney.head.description shouldBe "Hello world journey 3 - runIf [label-B]"
    }

    "include skip-if labels in description when specified in journey definition" in {
      val configUnderTest = new JourneyConfiguration {}
      val actualJourney   = configUnderTest.definitions(Set("label-B")).filter(_.id == "hello-world-2")
      actualJourney                    should have size 1
      actualJourney.head.description shouldBe "Hello world journey 2 - skipIf [label-A,label-C]"
    }

    "include run-if and skip-if labels in description when specified in journey definition" in {
      val configUnderTest = new JourneyConfiguration {}
      val actualJourney   = configUnderTest.definitions(Set("label-B")).filter(_.id == "hello-world-4")
      actualJourney                    should have size 1
      actualJourney.head.description shouldBe "Hello world journey 4 - runIf [label-B] and skipIf [label-A,label-C]"
    }
  }

  "JourneyDefinition" should {

    val scenarios = Table(
      ("scenario", "runIf", "skipIf", "testLabels", "expectedResult"),
      ("runIf, shouldIf and testLabels empty", Set.empty[String], Set.empty[String], Set.empty[String], true),
      (
        "runIf and shouldIf empty, testLabels non-empty",
        Set.empty[String],
        Set.empty[String],
        Set[String]("A", "B"),
        true
      ),
      ("intersecting runIf and skipIf empty", Set[String]("A", "C"), Set.empty[String], Set[String]("A", "B"), false),
      (
        "runIf subset of test labels and skipIf empty",
        Set[String]("A", "C"),
        Set.empty[String],
        Set[String]("A", "B", "C"),
        true
      ),
      ("intersecting skipIf and runIf empty", Set.empty[String], Set[String]("A", "C"), Set[String]("A", "B"), false),
      ("non-matching runIf and matching skipIf", Set[String]("C", "D"), Set[String]("B"), Set[String]("A", "B"), false),
      (
        "runIf non-empty, shouldIf empty and testLabels empty",
        Set[String]("A", "B"),
        Set.empty[String],
        Set.empty[String],
        false
      )
    )

    forAll(scenarios) {
      (scenario: String, runIf: Set[String], skipIf: Set[String], testLabels: Set[String], expectedResult: Boolean) =>
        s"return whether it should be executed - scenario: $scenario" in {
          val journeyDefinition = JourneyDefinition("id", "desc", 0, List.empty, "feeder", runIf, skipIf)
          journeyDefinition.shouldRun(testLabels) shouldBe expectedResult
        }
    }

    "throw an exception when runIf and skipIf are overlapping" in {
      val journeyDefinition = JourneyDefinition("id", "desc", 0, List.empty, "feeder", Set("A", "B"), Set("B", "C"))
      val thrown            = intercept[RuntimeException] {
        journeyDefinition.shouldRun(Set.empty)
      }
      thrown.getMessage shouldBe "Invalid configuration for journey with id=id. 'run-if' and 'skip-if' can't overlap"
    }
  }
}
