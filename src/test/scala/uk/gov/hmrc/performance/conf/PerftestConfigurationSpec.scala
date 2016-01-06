/*
 * Copyright 2016 HM Revenue & Customs
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
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Properties

class PerftestConfigurationSpec extends UnitSpec {

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
