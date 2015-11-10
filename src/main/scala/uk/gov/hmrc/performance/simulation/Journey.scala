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

package uk.gov.hmrc.performance.simulation

import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.request.builder.HttpRequestBuilder


import io.gatling.core.Predef._

trait Journey {

  val load: Double

  def builder: ScenarioBuilder

}

case class JourneyPart(id: String, description: String) {

  val rb = scala.collection.mutable.MutableList[HttpRequestBuilder]()

  def builder: ChainBuilder =
    if (rb.isEmpty) throw new scala.IllegalArgumentException(s"Journey '$id' must have at least one request")
    else rb.tail.foldLeft(exec(rb.head))((ex, trb) => ex.exec(trb))

  def withRequests(requests: HttpRequestBuilder*): Unit = rb ++ requests
}
