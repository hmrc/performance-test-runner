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

trait JourneyConfiguration extends Configuration {


  def definitions: Seq[JourneyDefinition] = {
    keys("journeys").map(id => {
      val description = readProperty(s"journeys.$id.description")
      val load = readProperty(s"journeys.$id.load").toDouble
      val parts = readPropertyList(s"journeys.$id.parts")
      val feeder = readProperty(s"journeys.$id.feeder")
      JourneyDefinition(id, description, load, parts, feeder)
    })
  }

}

case class JourneyDefinition(id: String, description: String, load: Double, parts: List[String], feeder: String)
