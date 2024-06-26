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

import io.gatling.commons.util.CircularIterator
import io.gatling.commons.util.Io.withCloseable
import io.gatling.commons.validation.{Failure, Success}
import io.gatling.core.config.{GatlingConfiguration, GatlingFiles}
import io.gatling.core.feeder.{Feeder, Record, SeparatedValuesParser}
import io.gatling.core.util.ResourceCache

import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicLong
import scala.util.Random

/** Implements Gatling's Feeder to feed test data from a CSV file. The CSV records are available in Gatling's session
  * for use during the test.
  *
  * Example:
  * {{{
  * username,password
  * bob,12345678
  * alice,87654321}}}
  *
  * The provided CSV can also contain placeholders to generate dynamic data from a single record.
  *
  * Example with random placeholder:
  * {{{
  * username,password
  * my-${random}-user,12345678}}}
  *
  * In the above CSV, `${random}` is replaced with a random int value
  *
  * Other available placeholders:
  *
  * ${currentTime} - replaced with the current time in milliseconds
  *
  * ${range-X} - replaced by a string representation of number made of X digits.
  *
  * The number is incremental and starts from 1 again when it reaches the max value.
  * For example ${range-3} will be replaced with '001' the first time, '002' the next and so on.
  *
  * @constructor creates a new feeder from a CSV file
  * @param feederFile name of the feeder file with directory. Example: data/helloworld.csv.
  * @param configuration GatlingConfiguration provided implicitly
  */

class CsvFeeder(feederFile: String)(implicit configuration: GatlingConfiguration)
    extends Feeder[String]
    with ResourceCache {

  val regularCsvFeeder: Iterator[Record[String]] = {
    cachedResource(GatlingFiles.customResourcesDirectory(configuration), feederFile) match {
      case Success(res)     =>
        withCloseable(FileChannel.open(res.file.toPath)) { channel =>
          CircularIterator(
            SeparatedValuesParser
              .stream(columnSeparator = ',', quoteChar = '"', charset = configuration.core.charset)(channel)
              .toVector,
            threadSafe = true
          )
        }
      case Failure(message) => throw new IllegalArgumentException(s"Could not locate feeder file; $message")
    }
  }

  private val rng = new Random

  private val ranges: scala.collection.mutable.Map[Int, AtomicLong] = new scala.collection.mutable.HashMap()

  override def hasNext = true

  private val rangeStr = """.*(\$\{range-([\d]+)\}).*"""
  private val rangeR   = rangeStr.r

  def replaceRange(value: String): String =
    value match {
      case rangeR(range, lengthStr) =>
        val length = lengthStr.toInt
        if (!ranges.isDefinedAt(length)) ranges += (length -> new AtomicLong(1))

        val formatter  = s"%0${length}d"
        val rangeValue = formatter.format(ranges(length).longValue)

        value.replaceAll("""\$\{range-""" + length + """\}""", rangeValue)
      case _                        => value
    }

  def incrementRanges(): Unit =
    ranges.foreach { case (r, l) =>
      if (l.longValue < (math.pow(10, r) - 1)) ranges(r).incrementAndGet
      else ranges(r).set(1)
    }

  override def next(): Map[String, String] = {
    val record: Record[String] = regularCsvFeeder.next()
    val randomInt: String      = Math.abs(rng.nextInt()).toString
    val now: String            = System.currentTimeMillis().toString
    incrementRanges()

    record.map { case (k, v) =>
      val vRand: String  = v.toString.replaceAll("""\$\{random\}""", randomInt)
      val vTime: String  = vRand.toString.replaceAll("""\$\{currentTime\}""", now)
      val vRange: String = replaceRange(vTime)
      (k, vRange)
    }
  }
}
