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

package models.asp

import config.ApplicationConfig
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Asp(
                services: Option[ServicesOfBusiness] = None,
                otherBusinessTaxMatters: Option[OtherBusinessTaxMatters] = None,
                hasChanged: Boolean = false,
                hasAccepted: Boolean = false
              ) {

  def services(p: ServicesOfBusiness): Asp =
    this.copy(services = Some(p), hasChanged = hasChanged || !this.services.contains(p))

  def otherBusinessTaxMatters(p: OtherBusinessTaxMatters): Asp =
    this.copy(otherBusinessTaxMatters = Some(p), hasChanged = hasChanged || !this.otherBusinessTaxMatters.contains(p))

  def isComplete: Boolean = this match {
    case Asp(Some(_), Some(_), _, true) if ApplicationConfig.hasAcceptedToggle => true
    case Asp(Some(_), Some(_), _, false) if ApplicationConfig.hasAcceptedToggle => false
    case Asp(Some(_), Some(_), _, _) => true
    case _ => false
  }
}

object Asp {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val formatOption = Reads.optionWithNull[Asp]

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "asp"
    val notStarted = Section(messageKey, NotStarted, false, controllers.asp.routes.WhatYouNeedController.get())
    cache.getEntry[Asp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.asp.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.asp.routes.WhatYouNeedController.get())
        }
    }
  }

  val key = "asp"

  implicit val mongoKey = new MongoKey[Asp] {
    override def apply(): String = "asp"
  }

  implicit val jsonWrites = Json.writes[Asp]

  implicit val jsonReads: Reads[Asp] = {
    (__ \ "services").readNullable[ServicesOfBusiness] and
      (__ \ "otherBusinessTaxMatters").readNullable[OtherBusinessTaxMatters] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
  }.apply(Asp.apply _)

  implicit def default(details: Option[Asp]): Asp =
    details.getOrElse(Asp())
}
