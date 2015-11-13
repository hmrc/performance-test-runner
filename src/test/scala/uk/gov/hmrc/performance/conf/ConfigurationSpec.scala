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

import com.typesafe.config.ConfigFactory
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Properties

class ConfigurationSpec extends UnitSpec {


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

    "read local configurations" in {

      val configUnderTest = new Configuration {}
      configUnderTest.readProperty("services.helloworld-service.port") shouldBe "9000"
    }

    "read list of keys" in {

      val configUnderTest = new Configuration {}
      configUnderTest.keys("journeys") should contain ("hello-world-1")
      configUnderTest.keys("journeys") should contain ("hello-world-2")
      configUnderTest.keys("journeys") should contain ("hello-world-3")
    }
  }
}
