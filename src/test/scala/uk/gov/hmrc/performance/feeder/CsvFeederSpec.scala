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

package uk.gov.hmrc.performance.feeder

import io.gatling.core.config.GatlingConfiguration
import uk.gov.hmrc.play.test.UnitSpec

class CsvFeederSpec extends UnitSpec {

  GatlingConfiguration.setUpForTest()

  "The feeder" should {

    "return values by key" in {
      val feeder: CsvFeeder = new CsvFeeder("data/helloworld.csv")
      val next: Map[String, String] = feeder.next()
      next("username") shouldBe "bob"
      next("password") shouldBe "12345678"
    }

    "replace the random placeholder" in {
      val feeder: CsvFeeder = new CsvFeeder("data/helloworld.csv")
      val next: Map[String, String] = feeder.next()
      next("email") matches "^[0-9]*@somemail.com"
    }

    "replace the currentTime placeholder" in {
      val feeder: CsvFeeder = new CsvFeeder("data/helloworld.csv")
      val next: Map[String, String] = feeder.next()
      next("time") matches "^[0-9]*"
    }
  }

}
