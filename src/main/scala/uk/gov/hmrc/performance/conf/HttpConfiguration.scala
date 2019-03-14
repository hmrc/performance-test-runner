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

package uk.gov.hmrc.performance.conf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

trait HttpConfiguration extends Configuration {

  val headers = Map(
    """Accept""" -> """text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8""",
    """Cache-Control""" -> """no-cache""")

  val httpProtocol = http
    .acceptHeader("image/png,image/*;q=0.8,*/*;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-gb,en;q=0.5")
    .connectionHeader("close")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:25.0) Gecko/20100101 Firefox/25.0")
    .header("True-Client-IP", "${random}")
    .disableFollowRedirect

}
