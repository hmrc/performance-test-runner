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

  def checkJourneyName(id: String): Any =
    if (!allJourneys.contains(id))
      throw new RuntimeException(s"The test is configured to run '$id' but it couldn't be found in journeys.conf")

  /** Maps the journeys defined in journeys.conf to a Seq of
    *  [[uk.gov.hmrc.performance.conf.JourneyDefinition]] and returns the journeys to be executed
    *  based on the provided labels. The `labels` field is defined in application.conf.
    *
    * @param labels An optional parameter with a Set of labels for which the journeys should be executed.
    * @return Seq of JourneyDefinition to be executed for the provided labels.
    */
  def definitions(labels: Set[String] = Set.empty): Seq[JourneyDefinition] = {
    val journeyDefinitions = journeysAvailable
      .map { id =>
        val journeyDescription = readProperty(s"journeys.$id.description")
        val load               = readProperty(s"journeys.$id.load").toDouble
        val parts              = readPropertyList(s"journeys.$id.parts")
        val feeder             = readPropertyOption(s"journeys.$id.feeder").getOrElse("")
        val runIf              = readPropertySetOrEmpty(s"journeys.$id.run-if")
        val skipIf             = readPropertySetOrEmpty(s"journeys.$id.skip-if")
        val description        = generateDescription(journeyDescription, runIf, skipIf)
        JourneyDefinition(id, description, load, parts, feeder, runIf, skipIf)
      }
    journeyDefinitions.filter(journeyDefinition => journeyDefinition.shouldRun(labels))
  }

  /** Generates a new description for a journey based on the description, run-if, and skip-if fields in journeys.conf
    *
    * Example:
    *
    * For the below example journey in journey.conf:
    * {{{
    * hello-world-4 = {
    *   description = "Hello world journey 4"
    *   load = 0.1
    *   feeder = data/helloworld.csv
    *   parts = [
    *     home
    *   ]
    *   run-if = ["label-B"]
    *   skip-if = ["label-A", "label-C"]
    * }
    * }}}
    * the generated description would be:
    *
    * `Hello world journey 4 - runIf [label-B] and skipIf [label-A,label-C]`
    *
    * @param journeyDescription Value from the description field of a journey in journeys.conf
    * @param runIf Labels included in run-if field of a journey in journeys.conf
    * @param skipIf Labels included in skip-if field of a journey in journeys.conf
    * @return A new description based on the description and the labels
    */
  def generateDescription(journeyDescription: String, runIf: Set[String], skipIf: Set[String]): String = {
    val runIfDescription  = if (runIf.nonEmpty) "runIf " + s"[${runIf.mkString(",")}]" else ""
    val skipIfDescription = if (skipIf.nonEmpty) "skipIf " + s"[${skipIf.mkString(",")}]" else ""
    val and               = if (runIf.nonEmpty && skipIf.nonEmpty) " and " else ""
    val labels            = runIfDescription + and + skipIfDescription
    val separator         = if (labels.nonEmpty) " - " else ""
    journeyDescription + separator + labels
  }
}

case class JourneyDefinition(
  id: String,
  description: String,
  load: Double,
  parts: List[String],
  feeder: String,
  runIf: Set[String] = Set.empty,
  skipIf: Set[String] = Set.empty
) {

  /** Checks the run-if and skip-if labels that have been defined within journeys.conf
    * for a journey to determine if it has been configured correctly and should be executed.
    *
    * @param testLabels Set of labels as defined in application.conf
    * @return Boolean, whether it should be executed
    */
  def shouldRun(testLabels: Set[String]): Boolean = {
    if (runIf.intersect(skipIf).nonEmpty)
      throw new scala.RuntimeException(
        s"Invalid configuration for journey with id=$id. 'run-if' and 'skip-if' can't overlap"
      )
    if (skipIf.intersect(testLabels).nonEmpty) false
    else runIf.isEmpty || runIf.subsetOf(testLabels)
  }
}
