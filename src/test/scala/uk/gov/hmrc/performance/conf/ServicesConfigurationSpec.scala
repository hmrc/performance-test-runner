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

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}

import scala.util.Properties

class ServicesConfigurationSpec extends WordSpec with Matchers with ServicesConfiguration {

  "ServicesConfiguration" should {

    "return the url from the conf file when the service name is found" in {

      // given
      val someService = "helloworld-service"

      val expectedBaseUrl = "http://localhost:9000"

      // when
      val baseUrl: String = baseUrlFor(someService)

      // then
      baseUrl shouldBe expectedBaseUrl
    }

    "return the base url if the service is not found" in {

      // given
      val someService = "incorrectservicename "

      val expectedBaseUrl = "http://helloworld-service.co.uk"

      // when
      val baseUrl: String = baseUrlFor(someService)

      // then
      baseUrl shouldBe expectedBaseUrl
    }

    "read services-local configurations if runLocal = true" in {

      val configUnderTest = new ServicesConfiguration {}
      configUnderTest.baseUrlFor("helloworld-service") shouldBe "http://localhost:9000"
    }

    "read services configurations if runLocal = false" in {

      Properties.setProp("runLocal", "false")
      ConfigFactory.invalidateCaches()

      val configUnderTest = new ServicesConfiguration {}
      configUnderTest.baseUrlFor("helloworld-service") shouldBe "http://internal.helloworld-service.co.uk"

      Properties.clearProp("runLocal")
      ConfigFactory.invalidateCaches()
    }
  }

}
