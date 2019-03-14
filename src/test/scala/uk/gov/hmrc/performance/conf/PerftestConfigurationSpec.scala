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

import scala.concurrent.duration._
import scala.util.Properties

class PerftestConfigurationSpec extends WordSpec with Matchers {

  "TestRateConfiguration defaults" should {

    val perftestConfiguration = new PerftestConfiguration {
      override lazy val applicationConfig: Config = ConfigFactory.empty()
    }

    "read the ramp up time" in {
      perftestConfiguration.rampUpTime shouldBe (1 minute)
    }

    "read the constant rate time" in {
      perftestConfiguration.constantRateTime shouldBe (5 minutes)
    }

    "read the ramp down time" in {
      perftestConfiguration.rampDownTime shouldBe (1 minute)
    }

    "read the load percentage" in {
      perftestConfiguration.loadPercentage shouldBe 1D
    }
    "read the percentage failure threshold" in {
      perftestConfiguration.percentageFailureThreshold shouldBe 1
    }
  }


  "TestRateConfiguration via provided test config file" should {

    val perftestConfiguration = new PerftestConfiguration {

      import collection.JavaConverters._

      override lazy val applicationConfig: Config = ConfigFactory.parseMap(Map(
        "perftest.rampupTime" -> 123,
        "perftest.constantRateTime" -> 62,
        "perftest.rampdownTime" -> 21,
        "perftest.loadPercentage" -> 223,
        "perftest.runSmokeTest" -> false,
        "perftest.labels" -> "W, Z",
        "perftest.percentageFailureThreshold" -> 19
      ).asJava)
    }

    "read the ramp up time from the config file" in {
      perftestConfiguration.rampUpTime shouldBe (123 minute)
    }

    "read the constant rate time" in {
      perftestConfiguration.constantRateTime shouldBe (62 minutes)
    }

    "read the ramp down time" in {
      perftestConfiguration.rampDownTime shouldBe (21 minute)
    }

    "read the load percentage" in {
      perftestConfiguration.loadPercentage shouldBe 2.23
    }
    "read the percentage failure threshold" in {
      perftestConfiguration.percentageFailureThreshold shouldBe 19
    }
  }

  "PerftestConfiguration" should {

    val scenarios = Table(
      ("scenario", "labelsValue", "expectedValue"),
      ("property present", Some("A,B,C"), Set[String]("A", "B", "C")),
      ("property present with empty values", Some("A,,B,C,"), Set[String]("A", "B", "C")),
      ("property present with duplicate values", Some("A,A,B,C"), Set[String]("A", "B", "C")),
      ("property present but empty", Some(""), Set.empty[String]),
      ("property not present", None, Set.empty[String])
    )

    forAll(scenarios) { (scenario: String, labelsValue: Option[String], expectedValue: Set[String]) =>

      s"read test labels as list of string - scenario: $scenario" in {

        labelsValue.foreach(value => Properties.setProp("perftest.labels", value))
        ConfigFactory.invalidateCaches()

        val configuration = new PerftestConfiguration {}
        configuration.labels shouldBe expectedValue

        Properties.clearProp("perftest.labels")
        ConfigFactory.invalidateCaches()
      }
    }
  }
}
