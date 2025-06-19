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

package uk.gov.hmrc.performance.feeder

import io.gatling.commons.validation.{Failure, Success}
import io.gatling.core.Predef.csv
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.feeder
import io.gatling.core.feeder.Feeder
import io.gatling.core.util.ResourceCache

import java.util.concurrent.atomic.AtomicLong
import scala.util.Random

/** Implements Gatling's Feeder to feed test data from a CSV file. The CSV records are available in Gatling's session
  * for use during the test.
  *
  * Example:
  * {{{
  * username,password
  * bob,12345678
  * alice,87654321
  * }}}
  *
  * The provided CSV can also contain placeholders to generate dynamic data from a single record.
  *
  * Example with random placeholder:
  * {{{
  * username,password
  * my-${random}-user,12345678
  * }}}
  *
  * In the above CSV, `${random}` is replaced with a random int value
  *
  * Other available placeholders:
  *
  * ${currentTime} - replaced with the current time in milliseconds
  *
  * ${range-X} - replaced by a string representation of number made of X digits.
  *
  * The number is incremental and starts from 1 again when it reaches the max value. For example ${range-3} will be
  * replaced with '001' the first time, '002' the next and so on.
  *
  * @constructor
  *   creates a new feeder from a CSV file
  * @param feederFile
  *   name of the feeder file with directory. Example: data/helloworld.csv.
  * @param configuration
  *   GatlingConfiguration provided implicitly
  */

class CsvFeeder(feederFile: String)(implicit configuration: GatlingConfiguration)
    extends Feeder[String]
    with ResourceCache {

  private val randomGenerator = new Random()

  private val ranges: scala.collection.mutable.Map[Int, AtomicLong] = new scala.collection.mutable.HashMap()

  private val rangeRegex = """.*(\$\{range-([\d]+)\}).*""".r

  override def hasNext = true

  private val regularCsvFeeder: Feeder[Any] =
    cachedResource(feederFile) match {
      case Success(_)       => csv(feederFile).circular.apply()
      case Failure(message) => throw new IllegalArgumentException(s"Could not locate feeder file; $message")
    }

  private def replaceRange(value: String): String =
    value match {
      case rangeRegex(_, rangeLength) =>
        val length = rangeLength.toInt
        if (!ranges.isDefinedAt(length)) ranges += (length -> new AtomicLong(1))

        val formatter  = s"%0${length}d"
        val rangeValue = formatter.format(ranges(length).longValue)

        value.replaceAll("""\$\{range-""" + length + """\}""", rangeValue)
      case _                          => value
    }

  private def incrementRanges(): Unit =
    ranges.foreach { case (r, l) =>
      val maxValue = math.pow(10, r) - 1
      if (l.longValue < maxValue) ranges(r).incrementAndGet else ranges(r).set(1)
    }

  override def next(): Map[String, String] = {
    val record: feeder.Record[Any] = regularCsvFeeder.next()
    val randomInt: String          = randomGenerator.nextInt().toString
    val now: String                = System.currentTimeMillis().toString
    incrementRanges()

    record.map { case (k, v) =>
      val vRand: String  = v.toString.replaceAll("""\$\{random\}""", randomInt)
      val vTime: String  = vRand.replaceAll("""\$\{currentTime\}""", now)
      val vRange: String = replaceRange(vTime)
      (k, vRange)
    }
  }
}
