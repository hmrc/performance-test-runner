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

import io.gatling.core.config.Resource
import io.gatling.core.feeder.{SeparatedValuesParser, Feeder, JsonFeederFileParser, Record}
import io.gatling.core.util.RoundRobin
import io.gatling.core.validation.{Failure, Success}

import scala.util.Random

class CsvFeeder(feederFile: String) extends Feeder[String] {

  val regularCsvFeeder = {
    Resource.feeder(feederFile) match {
      case Success(res) => RoundRobin(SeparatedValuesParser.parse(resource = res, separator = ',', doubleQuote = '"', rawSplit = false))
      case Failure(message) => throw new IllegalArgumentException(s"Could not locate feeder file; $message")
    }
  }

  private val rng = new Random

  override def hasNext = true

  override def next(): Map[String, String] = {
    val record: Record[String] = regularCsvFeeder.next()

    record.map {
      case (k, v) => {
        val vRand: String = v.toString.replaceAll("""\$\{random\}""", rng.nextInt().toString)
        val vTime: String = vRand.toString.replaceAll("""\$\{currentTime\}""", System.currentTimeMillis().toString)
        (k, vTime)
      }
    }

  }
}
