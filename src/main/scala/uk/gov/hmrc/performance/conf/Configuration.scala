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

import com.typesafe.config.{Config, ConfigFactory, ConfigMergeable}

import scala.util.Properties


trait Configuration {

  import scala.collection.JavaConverters._

  private val defaultConfig = ConfigFactory.systemProperties().withFallback(ConfigFactory.load())

  private val localConfig: ConfigMergeable =
    try {
      ConfigFactory.load("services-local")
    } catch {
      case _: Exception => throw new RuntimeException("Couldn't load services-local.conf. Please check the configuration file")
    }

  lazy val runLocal: Boolean =
    try {
      defaultConfig.getBoolean("runLocal")
    } catch {
      case _: Exception => throw new RuntimeException("Couldn't load the value runLocal. Please check the runLocal value in the application.conf file")
    }

  lazy val applicationConfig: Config = {
    if (runLocal)
      defaultConfig.withFallback(localConfig)
    else defaultConfig
  }

  def readProperty(property: String): String = {
    Properties.propOrElse(property, applicationConfig.getString(property))
  }

  def readPropertyList(property: String) = applicationConfig.getStringList(property).asScala.toList

  def keys(property: String) = applicationConfig.getObject("journeys").keySet().asScala.toList
}
