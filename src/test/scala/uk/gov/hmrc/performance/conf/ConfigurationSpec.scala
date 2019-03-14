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

class ConfigurationSpec extends WordSpec with Matchers {


  "Configuration" should {

    "read runLocal default value" in {

      val configUnderTest = new Configuration {}
      configUnderTest.runLocal shouldBe true
    }

    "read a simple property" in {

      val configUnderTest = new Configuration {}
      configUnderTest.readProperty("baseUrl") shouldBe "http://helloworld-service.co.uk"
    }


    "give priority to environment variables" in {

      Properties.setProp("baseUrl", "anotherBaseUrl")
      ConfigFactory.invalidateCaches()

      val configUnderTest = new Configuration {}
      configUnderTest.readProperty("baseUrl") shouldBe "anotherBaseUrl"

      Properties.clearProp("baseUrl")
      ConfigFactory.invalidateCaches()
    }

    "read services-local configurations if runLocal = true" in {

      val configUnderTest = new Configuration {}
      configUnderTest.readProperty("services.helloworld-service.port") shouldBe "9000"
    }

    "read services configurations if runLocal = false" in {

      Properties.setProp("runLocal", "false")
      ConfigFactory.invalidateCaches()

      val configUnderTest = new Configuration {}
      configUnderTest.readProperty("services.helloworld-service.host") shouldBe "internal.helloworld-service.co.uk"

      Properties.clearProp("runLocal")
      ConfigFactory.invalidateCaches()
    }

    "read list of keys" in {

      val configUnderTest = new Configuration {}
      configUnderTest.keys("journeys") should contain ("hello-world-1")
      configUnderTest.keys("journeys") should contain ("hello-world-2")
      configUnderTest.keys("journeys") should contain ("hello-world-3")
    }

    "throw an exception if the key is not found" in {
      val configUnderTest = new Configuration {}
      intercept[Exception] {
        configUnderTest.readProperty("iDoNotExist")
      }
    }

    "return the default if the key is not found and a default is provided" in {
      val configUnderTest = new Configuration {}
      configUnderTest.readProperty("iDoNotExist", "imTheDefault") shouldBe "imTheDefault"
    }

    "read a property as set of string" in {
      val configUnderTest = new Configuration {}
      configUnderTest.readPropertySet("journeys.hello-world-3.run-if") shouldBe Set("label-B")
      configUnderTest.readPropertySetOrEmpty("journeys.hello-world-3.run-if") shouldBe Set("label-B")
      configUnderTest.readPropertySetOrEmpty("journeys.hello-world-1.run-if") shouldBe Set.empty
    }
  }
}
