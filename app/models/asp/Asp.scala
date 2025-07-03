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

package models.asp

import models.registrationprogress._
import play.api.i18n.Messages
import typeclasses.MongoKey
import services.cache.Cache

case class Asp(
  services: Option[ServicesOfBusiness] = None,
  otherBusinessTaxMatters: Option[OtherBusinessTaxMatters] = None,
  hasChanged: Boolean = false,
  hasAccepted: Boolean = false
) {

  def services(p: ServicesOfBusiness): Asp =
    this.copy(
      services = Some(p),
      hasChanged = hasChanged || !this.services.contains(p),
      hasAccepted = hasAccepted && this.services.contains(p)
    )

  def otherBusinessTaxMatters(p: OtherBusinessTaxMatters): Asp =
    this.copy(
      otherBusinessTaxMatters = Some(p),
      hasChanged = hasChanged || !this.otherBusinessTaxMatters.contains(p),
      hasAccepted = hasAccepted && this.otherBusinessTaxMatters.contains(p)
    )

  def isComplete: Boolean = this match {
    case Asp(Some(_), Some(_), _, accepted) => accepted
    case _                                  => false
  }
}

object Asp {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def taskRow(implicit cache: Cache, messages: Messages): TaskRow = {
    val messageKey = "asp"
    val notStarted = TaskRow(
      messageKey,
      controllers.asp.routes.WhatYouNeedController.get.url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )
    val respUrl    = controllers.asp.routes.SummaryController.get.url
    cache.getEntry[Asp](key).fold(notStarted) { model =>
      if (model.isComplete && model.hasChanged) {
        TaskRow(
          key,
          controllers.asp.routes.SummaryController.get.url,
          hasChanged = true,
          status = Updated,
          tag = TaskRow.updatedTag
        )
      } else if (model.isComplete) {
        TaskRow(
          messageKey,
          controllers.routes.YourResponsibilitiesUpdateController.get(respUrl).url,
          model.hasChanged,
          Completed,
          TaskRow.completedTag
        )
      } else {
        TaskRow(
          messageKey,
          controllers.asp.routes.WhatYouNeedController.get.url,
          model.hasChanged,
          Started,
          TaskRow.incompleteTag
        )
      }
    }
  }

  val key = "asp"

  implicit val mongoKey: MongoKey[Asp] = new MongoKey[Asp] {
    override def apply(): String = "asp"
  }

  implicit val jsonWrites: OWrites[Asp] = Json.writes[Asp]

  implicit val jsonReads: Reads[Asp] = {
    (__ \ "services").readNullable[ServicesOfBusiness] and
      (__ \ "otherBusinessTaxMatters").readNullable[OtherBusinessTaxMatters] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
  }.apply(Asp.apply _)

  implicit val formatOption: Reads[Option[Asp]] = Reads.optionWithNull[Asp]

  implicit def default(details: Option[Asp]): Asp =
    details.getOrElse(Asp())
}
