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

package uk.gov.hmrc.performance.conf

import scala.util.Try

trait PerftestConfiguration extends Configuration {
  val noLoad = 0.0001D

  import scala.concurrent.duration._

  lazy val rampUpTime: FiniteDuration = readProperty("perftest.rampupTime").toInt minutes
  lazy val rampDownTime: FiniteDuration = readProperty("perftest.rampdownTime").toInt minutes
  lazy val constantRateTime: FiniteDuration = readProperty("perftest.constantRateTime").toInt minutes
  lazy val loadPercentage: Double = readProperty("perftest.loadPercentage").toDouble / 100D
  lazy val runSingleUserJourney: Boolean = Try(readProperty("perftest.singleJourney").toBoolean).getOrElse(false)

}
