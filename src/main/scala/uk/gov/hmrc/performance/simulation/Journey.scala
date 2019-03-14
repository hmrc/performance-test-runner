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

package uk.gov.hmrc.performance.simulation

import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Expression
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.request.builder.HttpRequestBuilder

trait Journey {
  val load: Double

  def builder: ScenarioBuilder
}

case class JourneyPart(id: String, description: String) {

  val ab = scala.collection.mutable.MutableList[ActionBuilder]()
  var conditionallyRun: ChainBuilder => ChainBuilder = (cb) => cb

  def builder: ChainBuilder =
    if (ab.isEmpty) throw new scala.IllegalArgumentException(s"'$id' must have at least one request")
    else conditionallyRun(ab.tail.foldLeft(exec(ab.head))((ex, trb) => ex.exec(trb)))

  def withRequests(requests: HttpRequestBuilder*): JourneyPart = {
    ab ++= requests.map(r => HttpRequestBuilder.toActionBuilder(r))
    this
  }

  def withActions(actions: ActionBuilder*): JourneyPart = {
    ab ++= actions
    this
  }

  def toRunIf(sessionKey: Expression[String], value: String): JourneyPart = {
    conditionallyRun = doIfEquals(sessionKey, value)
    this
  }
}
