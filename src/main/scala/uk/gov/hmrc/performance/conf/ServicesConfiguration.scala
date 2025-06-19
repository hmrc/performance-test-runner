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

package uk.gov.hmrc.performance.conf

trait ServicesConfiguration extends Configuration {

  private def urlFor(protocol: String, host: String, port: String) =
    if (port.toInt == 80 || port.toInt == 443) s"$protocol://$host" else s"$protocol://$host:$port"

  /** Returns a baseUrl for the provided serviceName based on the serviceName configuration in `services.conf` or
    * services-local.conf` when running locally.
    * @param serviceName
    * @return
    *   baseUrl for the service as a String.
    */
  def baseUrlFor(serviceName: String): String = {
    val protocol = readProperty(s"services.$serviceName.protocol", "")
    val host     = readProperty(s"services.$serviceName.host", "")
    val port     = readProperty(s"services.$serviceName.port", "")

    if (serviceIsDefined(protocol, host, port)) {

      val protocolOrDefault = if (protocol.isEmpty) "http" else protocol
      val hostOrDefault     = if (host.isEmpty) "localhost" else host
      val portOrDefault     = if (port.isEmpty) "80" else port

      urlFor(protocolOrDefault, hostOrDefault, portOrDefault)
    } else {
      val confFile =
        if (runLocal)
          "services-local.conf"
        else
          "services.conf"
      throw ConfigNotFoundException(s"'$serviceName' not defined in '$confFile'.")
    }
  }

  def serviceIsDefined(protocol: String, host: String, port: String): Boolean =
    !protocol.isEmpty || !host.isEmpty || !port.isEmpty

  case class ConfigNotFoundException(message: String) extends RuntimeException(message)
}
