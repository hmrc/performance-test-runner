/*
 * Copyright 2023 HM Revenue & Customs
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

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

trait HttpConfiguration extends Configuration {

  /** Provides a `HttpProtocolBuilder` with preset configuration. This configures the HTTP protocol used in Gatling simulation.
    *
    * https://gatling.io/docs/3.4/http/http_protocol/
    *
    * Configurations to note:
    *
    * `True-Client-IP`:  Set to `java.util.Random()` value injected in the Gatling session during JourneySetup.journeys.
    * This value can be used to trace requests injected through the performance-test-runner library.
    *
    * `disableFollowRedirect`: Disables Gatling from following redirect automatically.This means the users of the library
    * should make explicit redirect requests. https://gatling.io/docs/3.4/http/http_protocol/#response-handling-parameters
    *
    * Users of the library can override this default configuration by overriding `val httpProtocol` when extending `PerformanceTestRunner` trait.
    */

  val httpProtocol: HttpProtocolBuilder = http
    .acceptHeader("image/png,image/*;q=0.8,*/*;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-gb,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:25.0) Gecko/20100101 Firefox/25.0")
    .header("True-Client-IP", "${random}")
    .disableFollowRedirect
}
