/*
 * Copyright 2018 HM Revenue & Customs
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
import org.scalatest.{Matchers, WordSpec}

class CsvFeederSpec extends WordSpec with Matchers {

  implicit val configuration = GatlingConfiguration.loadForTest()

  "The feeder" should {

    "return values by key" in {
      val feeder: CsvFeeder = new CsvFeeder("data/helloworld.csv")
      val next: Map[String, String] = feeder.next()
      next("username") shouldBe "bob"
      next("password") shouldBe "12345678"
    }

    "create only one random per request" in {
      val feeder: CsvFeeder = new CsvFeeder("data/randomFeeder.csv")
      val next: Map[String, String] = feeder.next()
      next("username") shouldBe next("password")
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

    "replace the range place holder" in {
      val feeder: CsvFeeder = new CsvFeeder("data/range.csv")
      feeder.next()("username") shouldBe "bob-000001"
      feeder.next()("username") shouldBe "bob-000002"
    }
  }

  "the range" should {
    "be reused in the same journey" in {
      val feeder: CsvFeeder = new CsvFeeder("data/range.csv")
      val next: Map[String, String] = feeder.next()
      next("username") shouldBe "bob-000001"
      next("password") shouldBe "000001"
    }

    "should work in the middle of a string" in {
      val feeder: CsvFeeder = new CsvFeeder("data/range.csv")
      val next: Map[String, String] = feeder.next()
      next("withRangeInTheMiddle") shouldBe "90000019"
    }

    "restart from 1 once max is reached" in {
      val feeder: CsvFeeder = new CsvFeeder("data/range.csv")
      feeder.next()("other") shouldBe "1"
      feeder.next()("other") shouldBe "2"
      feeder.next()("other") shouldBe "3"
      feeder.next()("other") shouldBe "4"
      feeder.next()("other") shouldBe "5"
      feeder.next()("other") shouldBe "6"
      feeder.next()("other") shouldBe "7"
      feeder.next()("other") shouldBe "8"
      feeder.next()("other") shouldBe "9"
      feeder.next()("other") shouldBe "1"
    }

    "use different counters for different range sizes" in {
      val feeder: CsvFeeder = new CsvFeeder("data/range.csv")
      feeder.next()("other") shouldBe "1"
      feeder.next()("other") shouldBe "2"
      feeder.next()("other") shouldBe "3"
      feeder.next()("other") shouldBe "4"
      feeder.next()("other") shouldBe "5"
      feeder.next()("other") shouldBe "6"
      feeder.next()("other") shouldBe "7"
      feeder.next()("other") shouldBe "8"
      feeder.next()("other") shouldBe "9"

      val next: Map[String, String] = feeder.next()
      next("other") shouldBe "1"
      next("password") shouldBe "000010"
    }
  }
}
