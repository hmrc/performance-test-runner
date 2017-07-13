/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.concurrent.atomic.AtomicLong

import io.gatling.core.config.Resource
import io.gatling.core.feeder.{Feeder, Record, SeparatedValuesParser}
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

  private val ranges: scala.collection.mutable.Map[Int, AtomicLong] = new scala.collection.mutable.HashMap()

  override def hasNext = true

  private val rangeStr = """.*(\$\{range-([\d]+)\}).*"""
  private val rangeR = rangeStr.r


  def replaceRange(value: String): String = {
    value match {
      case rangeR(range, lengthStr) => {
        val length = lengthStr.toInt
        if (!ranges.isDefinedAt(length)) ranges += (length -> new AtomicLong(1))

        val formatter = s"%0${length}d"
        val rangeValue = formatter.format(ranges(length).longValue)

        value.replaceAll( """\$\{range-""" + length + """\}""", rangeValue)
      }
      case _ => value
    }
  }

  def incrementRanges(): Unit = {

    ranges.foreach {
      case (r, l) => {
        if (l.longValue < (math.pow(10, r) - 1)) ranges(r).incrementAndGet
        else ranges(r).set(1)
      }
    }
  }

  override def next(): Map[String, String] = {
    val record: Record[String] = regularCsvFeeder.next()
    val randomInt: String = Math.abs(rng.nextInt()).toString
    val now: String = System.currentTimeMillis().toString
    incrementRanges()

    record.map {
      case (k, v) => {
        val vRand: String = v.toString.replaceAll( """\$\{random\}""", randomInt)
        val vTime: String = vRand.toString.replaceAll( """\$\{currentTime\}""", now)
        val vRange: String = replaceRange(vTime)
        (k, vRange)
      }
    }
  }
}
