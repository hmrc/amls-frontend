/*
 * Copyright 2017 HM Revenue & Customs
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

package models.estateagentbusiness

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class EstateAgentBusiness(
                                services: Option[Services] = None,
                                redressScheme: Option[RedressScheme] = None,
                                professionalBody: Option[ProfessionalBody] = None,
                                penalisedUnderEstateAgentsAct: Option[PenalisedUnderEstateAgentsAct] = None,
                                hasChanged : Boolean = false
                              ) {
  def services(p: Services): EstateAgentBusiness =
    this.copy(services = Some(p), hasChanged = hasChanged || !this.services.contains(p))

  def redressScheme(p: RedressScheme): EstateAgentBusiness =
    this.copy(redressScheme = Some(p), hasChanged = hasChanged || !this.redressScheme.contains(p))

  def professionalBody(p: ProfessionalBody): EstateAgentBusiness =
    this.copy(professionalBody = Some(p), hasChanged = hasChanged || !this.professionalBody.contains(p))

  def penalisedUnderEstateAgentsAct(p: PenalisedUnderEstateAgentsAct): EstateAgentBusiness =
    this.copy(penalisedUnderEstateAgentsAct = Some(p), hasChanged = hasChanged || !this.penalisedUnderEstateAgentsAct.contains(p))

  def isComplete: Boolean =
    this match {
      case EstateAgentBusiness(Some(x), _, Some(_), Some(_), _) if !x.services.contains(Residential) => true
      case EstateAgentBusiness(Some(_), Some(_), Some(_), Some(_), _) => true
      case _ => false
    }
}

object EstateAgentBusiness {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "eab"
    val notStarted = Section(messageKey, NotStarted, false, controllers.estateagentbusiness.routes.WhatYouNeedController.get())

    cache.getEntry[EstateAgentBusiness](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.estateagentbusiness.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.estateagentbusiness.routes.WhatYouNeedController.get())
        }
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "estate-agent-business"

  implicit val formatOption = Reads.optionWithNull[EstateAgentBusiness]

  implicit val reads: Reads[EstateAgentBusiness] = (
    __.read(Reads.optionNoError[Services]) and
      __.read(Reads.optionNoError[RedressScheme]) and
      __.read(Reads.optionNoError[ProfessionalBody]) and
      __.read(Reads.optionNoError[PenalisedUnderEstateAgentsAct]) and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false))
    ) (EstateAgentBusiness.apply _)

  implicit val writes: Writes[EstateAgentBusiness] =
    Writes[EstateAgentBusiness] {
      model =>
        Seq(
          Json.toJson(model.services).asOpt[JsObject],
          Json.toJson(model.redressScheme).asOpt[JsObject],
          Json.toJson(model.professionalBody).asOpt[JsObject],
          Json.toJson(model.penalisedUnderEstateAgentsAct).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        } + ("hasChanged" -> JsBoolean(model.hasChanged))
    }

  implicit def default(aboutYou: Option[EstateAgentBusiness]): EstateAgentBusiness =
    aboutYou.getOrElse(EstateAgentBusiness())
}
