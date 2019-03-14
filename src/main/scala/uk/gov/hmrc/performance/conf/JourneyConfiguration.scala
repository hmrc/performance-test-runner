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
    val journeys = journeysAvailable
      .filter(id => !readPropertyBooleanOption(s"journeys.$id.abstract").getOrElse(false))
      .map(id => {
        val journeyToExtend = readPropertyOption(s"journeys.$id.extends")
        val abstractDescription = journeyToExtend.map { abstractJourneyId =>
          validateExtendedJourney(abstractJourneyId)
          readProperty(s"journeys.$abstractJourneyId.description")
        }.getOrElse(readProperty(s"journeys.$id.description"))
        val load = readProperty(s"journeys.$id.load").toDouble
        val parts = journeyToExtend.map(abstractJourneyId => readPropertyList(s"journeys.$abstractJourneyId.parts")).getOrElse(readPropertyList(s"journeys.$id.parts"))
        val feeder = readPropertyOption(s"journeys.$id.feeder").getOrElse(journeyToExtend.flatMap(abstractJourneyId => readPropertyOption(s"journeys.$abstractJourneyId.feeder")).getOrElse(""))
        val runIf = readPropertySetOrEmpty(s"journeys.$id.run-if")
        val skipIf = readPropertySetOrEmpty(s"journeys.$id.skip-if")
        val description = generateDescription(abstractDescription, runIf, skipIf)
        JourneyDefinition(id, description, load, parts, feeder, runIf, skipIf)
      })
    journeys.filter(definition => definition.shouldRun(labels))
  }

  def validateExtendedJourney(abstractJourneyId: String): Unit = {
    if (!hasProperty(s"journeys.$abstractJourneyId")) {
      throw new scala.RuntimeException(s"the abstract journey $abstractJourneyId is not defined")
    }
    if (hasProperty(s"journeys.$abstractJourneyId.extends")) {
      throw new scala.RuntimeException(s"the abstract journey $abstractJourneyId should not extend any other journey")
    }
    val extendedJourneyIsAbstract = readPropertyBooleanOption(s"journeys.$abstractJourneyId.abstract")
    if (!extendedJourneyIsAbstract.getOrElse(false)) {
      throw new scala.RuntimeException(s"the extended journey $abstractJourneyId should be abstract")
    }
  }

  def generateDescription(abstractDescription: String, runIf: Set[String], skipIf: Set[String]): String = {
    val runIfDescription = if (runIf.nonEmpty) "runIf " + s"[${runIf.mkString(",")}]" else ""
    val skipIfDescription = if (skipIf.nonEmpty) "skipIf " + s"[${skipIf.mkString(",")}]" else ""
    val and = if (runIf.nonEmpty && skipIf.nonEmpty) " and " else ""
    val labels = runIfDescription + and + skipIfDescription
    val separator = if (labels.nonEmpty) " - " else ""
    abstractDescription + separator + labels
  }
}

case class JourneyDefinition(id: String, description: String, load: Double, parts: List[String], feeder: String, runIf: Set[String] = Set.empty, skipIf: Set[String] = Set.empty) {
  def shouldRun(testLabels: Set[String]): Boolean = {
    if (runIf.intersect(skipIf).nonEmpty) throw new scala.RuntimeException(s"Invalid configuration for journey with id=$id. 'run-if' and 'skip-if' can't overlap")
    if (skipIf.intersect(testLabels).nonEmpty) false
    else runIf.isEmpty || runIf.subsetOf(testLabels)
  }
}
