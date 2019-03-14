/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.{Matchers, WordSpec}

import scala.collection.JavaConverters._
import scala.util.Properties

class JourneyConfigurationSpec extends WordSpec with Matchers {

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

      configUnderTest.definitions() should contain(JourneyDefinition(
        id = "hello-world-1",
        description = "Hello world journey 1",
        load = 9.1,
        parts = List("login", "home"),
        feeder = "data/helloworld.csv",
        runIf = Set.empty
      ))
    }

    "filter the definitions using test labels set if this is not empty" in {
      val configUnderTest = new JourneyConfiguration {}

      configUnderTest.definitions(Set("label-A")) shouldBe Seq(JourneyDefinition(
        id = "hello-world-1",
        description = "Hello world journey 1",
        load = 9.1,
        parts = List("login", "home"),
        feeder = "data/helloworld.csv",
        runIf = Set.empty
      ))
    }

    "be able to return only journeys set in the application.config file" in {

      Properties.setProp("journeysToRun.0", "hello-world-1")
      Properties.setProp("journeysToRun.1", "hello-world-3")
      ConfigFactory.invalidateCaches()
      val configUnderTest = new JourneyConfiguration {}

      configUnderTest.definitions() should contain theSameElementsAs Seq(JourneyDefinition(
        id = "hello-world-1",
        description = "Hello world journey 1",
        load = 9.1,
        parts = List("login", "home"),
        feeder = "data/helloworld.csv",
        runIf = Set.empty
      ))

      Properties.clearProp("journeysToRun.0")
      Properties.clearProp("journeysToRun.1")
      ConfigFactory.invalidateCaches()
    }

    "an abstract journey should not extend any other journey" in {

      val thrown = intercept[RuntimeException] {
        val configUnderTest = new JourneyConfiguration {
          override lazy val applicationConfig: Config = ConfigFactory.load("journeys").withFallback(ConfigFactory.parseMap(Map[String, String](
            "journeys.test-abstract-journey.abstract" -> "true",
            "journeys.test-abstract-journey.extends" -> "base-journey",
            "journeys.test-abstract-journey.load" -> "6",
            "journeys.test-journey.extends" -> "test-abstract-journey"
          ).asJava))
        }
        configUnderTest.definitions()
      }
      thrown.getMessage shouldBe "the abstract journey test-abstract-journey should not extend any other journey"
    }

    "an extended journey should be abstract - abstract key missing" in {

      val thrown = intercept[RuntimeException] {
        val configUnderTest = new JourneyConfiguration {
          override lazy val applicationConfig: Config = ConfigFactory.load("journeys").withFallback(ConfigFactory.parseMap(Map[String, String](
            "journeys.test-journey.extends" -> "hello-world-1"
          ).asJava))
        }
        configUnderTest.definitions()
      }
      thrown.getMessage shouldBe "the extended journey hello-world-1 should be abstract"
    }

    "an extended journey should be abstract - abstract key set to false" in {

      val thrown = intercept[RuntimeException] {
        val configUnderTest = new JourneyConfiguration {
          override lazy val applicationConfig: Config = ConfigFactory.load("journeys").withFallback(ConfigFactory.parseMap(Map[String, String](
            "journeys.test-journey.extends" -> "hello-world-1",
            "journeys.hello-world-1.abstract" -> "false"
          ).asJava))
        }
        configUnderTest.definitions()
      }
      thrown.getMessage shouldBe "the extended journey hello-world-1 should be abstract"
    }

    "an abstract journey should be defined" in {

      val thrown = intercept[RuntimeException] {
        val configUnderTest = new JourneyConfiguration {
          override lazy val applicationConfig: Config = ConfigFactory.load("journeys").withFallback(ConfigFactory.parseMap(Map[String, String](
            "journeys.test-journey.extends" -> "missing-journey"
          ).asJava))
        }
        configUnderTest.definitions()
      }
      thrown.getMessage shouldBe "the abstract journey missing-journey is not defined"
    }

    "the feeder defined in the non-abstract journey should override the one defined in the abstract one" in {

      val configUnderTest = new JourneyConfiguration {
        override lazy val applicationConfig: Config = ConfigFactory.load("journeys").withFallback(ConfigFactory.parseMap(Map[String, AnyRef](

            "journeys.abstract-journey.abstract" -> "true",
            "journeys.abstract-journey.description" -> "Some Journey",
            "journeys.abstract-journey.feeder" -> "data/helloworld.csv",
            "journeys.abstract-journey.parts" -> Set("login", "home").asJava,

            "journeys.test-journey.extends" -> "abstract-journey",
            "journeys.test-journey.load" -> "1",
            "journeys.test-journey.feeder" -> "data/feed-1.csv"
          ).asJava))
      }
      val journey = configUnderTest.definitions().find(_.description == "Some Journey")
      journey.isDefined shouldBe true
      journey.get.feeder shouldBe "data/feed-1.csv"

    }

    val scenarios = Table(
      ("scenario", "id", "expectedDescription", "expectedLoad", "expectedRunIf", "expectedSkipIf"),
      ("with no runIf/skipIf", "test-journey-4", "Base journey", 8, Set.empty[String], Set.empty[String]),
      ("with both runIf and skipIf", "test-journey-1", "Base journey - runIf [label-1] and skipIf [label-2,label-3]", 5, Set("label-1"), Set("label-2", "label-3")),
      ("with skipIf only", "test-journey-2", "Base journey - skipIf [label-2,label-3]", 6, Set.empty[String], Set("label-2", "label-3")),
      ("with runIf only", "test-journey-3", "Base journey - runIf [label-1]", 7, Set("label-1"), Set.empty[String])
    )

    forAll(scenarios) { (scenario, id, expectedDescription, expectedLoad, expectedRunIf, expectedSkipIf) =>
      s"a journey can extend an abstract one - $scenario" in {
        val configUnderTest = new JourneyConfiguration {
          override lazy val applicationConfig: Config = ConfigFactory.load("journeys").withFallback(ConfigFactory.parseMap(Map[String, AnyRef](
            "journeys.test-journey-1.extends" -> "base-journey",
            "journeys.test-journey-1.load" -> "5",
            "journeys.test-journey-1.run-if" -> Set("label-1").asJava,
            "journeys.test-journey-1.skip-if" -> Set("label-2", "label-3").asJava,

            "journeys.test-journey-2.extends" -> "base-journey",
            "journeys.test-journey-2.load" -> "6",
            "journeys.test-journey-2.skip-if" -> Set("label-2", "label-3").asJava,

            "journeys.test-journey-3.extends" -> "base-journey",
            "journeys.test-journey-3.load" -> "7",
            "journeys.test-journey-3.run-if" -> Set("label-1").asJava,

            "journeys.test-journey-4.extends" -> "base-journey",
            "journeys.test-journey-4.load" -> "8"
          ).asJava))
        }

        configUnderTest.definitions(Set("label-1")) should contain(JourneyDefinition(
          id = id,
          description = expectedDescription,
          load = expectedLoad,
          parts = List("login", "home"),
          feeder = "data/helloworld.csv",
          runIf = expectedRunIf,
          skipIf = expectedSkipIf
        ))
      }
    }
  }

  "JourneyDefinition" should {

    val scenarios = Table(
      ("scenario", "runIf", "skipIf", "testLabels", "expectedResult"),
      ("runIf, shouldIf and testLabels empty", Set.empty[String], Set.empty[String], Set.empty[String], true),
      ("runIf and shouldIf empty, testLabels non-empty", Set.empty[String], Set.empty[String], Set[String]("A", "B"), true),
      ("intersecting runIf and skipIf empty", Set[String]("A", "C"), Set.empty[String], Set[String]("A", "B"), false),
      ("runIf subset of test labels and skipIf empty", Set[String]("A", "C"), Set.empty[String], Set[String]("A", "B", "C"), true),
      ("intersecting skipIf and runIf empty", Set.empty[String], Set[String]("A", "C"), Set[String]("A", "B"), false),
      ("non-matching runIf and matching skipIf", Set[String]("C", "D"), Set[String]("B"), Set[String]("A", "B"), false),
      ("runIf non-empty, shouldIf empty and testLabels empty", Set[String]("A", "B"), Set.empty[String], Set.empty[String], false)
    )

    forAll(scenarios) { (scenario: String, runIf: Set[String], skipIf: Set[String], testLabels: Set[String], expectedResult: Boolean) =>

      s"return whether it should be executed - scenario: $scenario" in {
        val journeyDefinition = JourneyDefinition("id", "desc", 0, List.empty, "feeder", runIf, skipIf)
        journeyDefinition.shouldRun(testLabels) shouldBe expectedResult
      }
    }

    "throw an exception when runIf and skipIf are overlapping" in {
      val journeyDefinition = JourneyDefinition("id", "desc", 0, List.empty, "feeder", Set("A", "B"), Set("B", "C"))
      val thrown = intercept[RuntimeException] {
        journeyDefinition.shouldRun(Set.empty)
      }
      thrown.getMessage shouldBe "Invalid configuration for journey with id=id. 'run-if' and 'skip-if' can't overlap"
    }
  }
}
