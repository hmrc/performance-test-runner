/*
 * Copyright 2016 HM Revenue & Customs
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

  val allJourneys: List[String] = keys("journeys")

  val journeysAvailable: List[String] = {

    if (!hasProperty("journeysToRun")) allJourneys
    else {
      val values = readPropertyList("journeysToRun")
      if (values.isEmpty) throw new RuntimeException(s"journeysToRun is empty. Check your journeys.conf file")
        values.foreach((id: String) => checkJourneyName(id))
      values
    }
  }

  def checkJourneyName(id: String): Any = {
    if (!allJourneys.contains(id)) throw new RuntimeException(s"The test is configured to run '$id' but it couldn't be found in journeys.conf")
  }

  def definitions(labels: Set[String] = Set.empty): Seq[JourneyDefinition] = {
    val journeys = journeysAvailable.map(id => {
      val description = readProperty(s"journeys.$id.description")
      val load = readProperty(s"journeys.$id.load").toDouble
      val parts = readPropertyList(s"journeys.$id.parts")
      val feeder = readProperty(s"journeys.$id.feeder","")
      val runIf = readPropertySetOrEmpty(s"journeys.$id.run-if")
      val skipIf = readPropertySetOrEmpty(s"journeys.$id.skip-if")
      JourneyDefinition(id, description, load, parts, feeder, runIf, skipIf)
    })
    journeys.filter(definition => definition.shouldRun(labels))
  }
}

case class JourneyDefinition(id: String, description: String, load: Double, parts: List[String], feeder: String, runIf: Set[String] = Set.empty, skipIf: Set[String] = Set.empty) {
  def shouldRun(testLabels: Set[String]): Boolean = {
    if (runIf.intersect(skipIf).nonEmpty) throw new scala.RuntimeException(s"Invalid configuration for journey with id=$id. 'run-if' and 'skip-if' can't overlap")
    if (skipIf.intersect(testLabels).nonEmpty) false
    else runIf.isEmpty || runIf.intersect(testLabels).nonEmpty
  }
}
