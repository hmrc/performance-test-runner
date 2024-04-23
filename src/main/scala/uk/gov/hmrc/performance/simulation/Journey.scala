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

/** Used to constructs parts of a Journey as listed in journeys.conf. The JourneyPart is defined within a performance test using
  * uk.gov.hmrc.performance.simulation.JourneySetup.setup.
  *
  * To create a JourneyPart with uk.gov.hmrc.performance.simulation.JourneySetup.setup:
  * {{{
  * setup("login", "Login") withRequests (navigateToLoginPage, submitLogin)
  * }}}
  *
  * @param id A unique id which should match the parts listed in journeys.conf
  * @param description Description of the journey part surfaced in the gatling report.
  */
case class JourneyPart(id: String, description: String) {

  val ab                                             = scala.collection.mutable.ListBuffer[ActionBuilder]()
  var conditionallyRun: ChainBuilder => ChainBuilder = cb => cb

  /** Used internally by uk.gov.hmrc.performance.simulation.JourneySetup.journeys to chain the requests and actions
    * included in the JourneyPart.
    *
    * Applies [[conditionallyRun]] when a condition is specified in JourneyPart using [[toRunIf]].
    *
    * @return Requests and Actions of the JourneyPart as a `ChainBuilder`
    */
  def builder: ChainBuilder =
    if (ab.isEmpty) throw new scala.IllegalArgumentException(s"'$id' must have at least one request")
    else conditionallyRun(ab.tail.foldLeft(exec(ab.head))((ex, trb) => ex.exec(trb)))

  /** Chains HttpRequestBuilder to uk.gov.hmrc.performance.simulation.JourneySetup.setup.
    * {{{
    * setup("login-page", "Navigate to login page") withRequests navigateToLoginPage
    * }}}
    *
    * @param requests of type `HttpRequestBuilder`
    * @return JourneyPart for chaining additional requests, actions, and conditional runs
    */
  def withRequests(requests: HttpRequestBuilder*): JourneyPart = {
    ab ++= requests.map(r => HttpRequestBuilder.toActionBuilder(r))
    this
  }

  /** Chains ActionBuilders to a setup. ActionBuilder is what is passed to the DSL `exec()` method.
    *
    * For example, to add a PauseBuilder to a setup
    * {{{
    * val pause = new PauseBuilder(10 milliseconds, None)
    * setup("pause-action", "pauses for 10 milliseconds") withActions(pause)
    * }}}
    *  Since, HttpRequestBuilder is also an ActionBuilder, these can be chained together.
    *
    *  In the below example `navigateToLoginPage` and `submitLogin` are of type `HttpRequestBuilder`
    * {{{
    * val pause = new PauseBuilder(1 milliseconds, None)
    * setup("login", "Login") withActions(navigateToLoginPage, pause, submitLogin)
    * }}}
    *
    *  To include a ChainBuilder, use `.actionBuilders` to covert it into a `List[ActionBuilder]` and pass it to
    *  withActions as below.
    *
    *  In the below example, `sessionSetup` is of type `List[ActionBuilder]`
    * {{{
    * setup("session-setup", "Setting Session Value") withActions(sessionSetup:_*)
    * }}}
    * @param actions of type `ActionBuilder`
    * @return JourneyPart for chaining additional requests, actions, and conditional runs
    */

  def withActions(actions: ActionBuilder*): JourneyPart = {
    ab ++= actions
    this
  }

  /** Checks whether to run a setup step depending on whether the actual sessionKey value
    *  matches the expected value. The expected value can also be a Gatling sessionKey as the underlying
    *  doIfEquals method implicitly converts it into an Expression[Any].
    *
    * @param sessionKey the sessionKey to obtain the actual value.
    * @param value the expected value to compare.
    * @return JourneyPart
    */
  def toRunIf(sessionKey: Expression[String], value: String): JourneyPart = {
    conditionallyRun = doIfEquals(sessionKey, value)
    this
  }
}
