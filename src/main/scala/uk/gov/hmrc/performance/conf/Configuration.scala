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

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try


trait Configuration {

  import scala.collection.JavaConverters._

  private[conf] val defaultConfig = ConfigFactory.systemProperties().withFallback(ConfigFactory.load("journeys")).withFallback(ConfigFactory.load())

  lazy val runLocal: Boolean = !defaultConfig.hasPath("runLocal") || defaultConfig.getBoolean("runLocal")

  lazy val applicationConfig: Config = {
    if (runLocal)
      defaultConfig.withFallback(ConfigFactory.load("services-local"))
    else
      defaultConfig.withFallback(ConfigFactory.load("services"))
  }

  def hasProperty(property: String): Boolean = applicationConfig.hasPath(property)

  def readProperty(property: String): String = applicationConfig.getString(property)

  def readProperty(property: String, default: String): String = Try(applicationConfig.getString(property)).getOrElse(default)
  
  def readPropertyOption(property: String): Option[String] = Try(applicationConfig.getString(property)).toOption

  def readPropertyBooleanOption(property: String): Option[Boolean] = Try(applicationConfig.getBoolean(property)).toOption

  def readPropertyList(property: String): List[String] = applicationConfig.getStringList(property).asScala.toList

  def readPropertySet(property: String): Set[String] = applicationConfig.getStringList(property).asScala.toSet

  def readPropertySetOrEmpty(property: String): Set[String] = Try(applicationConfig.getStringList(property).asScala.toSet).getOrElse(Set.empty)

  def keys(property: String): List[String] = applicationConfig.getObject(property).keySet().asScala.toList
}
