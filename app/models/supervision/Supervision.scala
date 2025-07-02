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

package models.supervision

import models.registrationprogress._
import play.api.i18n.Messages
import typeclasses.MongoKey
import services.cache.Cache

case class Supervision(
  anotherBody: Option[AnotherBody] = None,
  professionalBodyMember: Option[ProfessionalBodyMember] = None,
  professionalBodies: Option[ProfessionalBodies] = None,
  professionalBody: Option[ProfessionalBody] = None,
  hasChanged: Boolean = false,
  hasAccepted: Boolean = false
) {

  def anotherBody(p: AnotherBody): Supervision =
    this.copy(
      anotherBody = Some(p),
      hasChanged = hasChanged || !this.anotherBody.contains(p),
      hasAccepted = hasAccepted && this.anotherBody.contains(p)
    )

  def professionalBodyMember(p: ProfessionalBodyMember): Supervision =
    this.copy(
      professionalBodyMember = Some(p),
      hasChanged = hasChanged || !this.professionalBodyMember.contains(p),
      hasAccepted = hasAccepted && this.professionalBodyMember.contains(p)
    )

  def professionalBodies(p: Option[ProfessionalBodies]): Supervision =
    this.copy(
      professionalBodies = p,
      hasChanged = hasChanged || !this.professionalBodies.equals(p),
      hasAccepted = hasAccepted && this.professionalBodies.equals(p)
    )

  def professionalBody(p: ProfessionalBody): Supervision =
    this.copy(
      professionalBody = Some(p),
      hasChanged = hasChanged || !this.professionalBody.contains(p),
      hasAccepted = hasAccepted && this.professionalBody.contains(p)
    )

  def isComplete: Boolean = this match {
    case Supervision(Some(AnotherBodyNo), Some(ProfessionalBodyMemberYes), Some(_), Some(_), _, true) => true

    case Supervision(Some(anotherBody), Some(ProfessionalBodyMemberYes), Some(_), Some(_), _, true)
        if anotherBody.asInstanceOf[AnotherBodyYes].isComplete() =>
      true

    case Supervision(Some(AnotherBodyNo), Some(ProfessionalBodyMemberNo), _, Some(_), _, true) => true

    case Supervision(Some(anotherBody), Some(ProfessionalBodyMemberNo), _, Some(_), _, true)
        if anotherBody.asInstanceOf[AnotherBodyYes].isComplete() =>
      true

    case _ => false
  }

  def isEmpty: Boolean = this match {
    case Supervision(None, None, None, None, _, _) => true
    case _                                         => false
  }
}

object Supervision {

  def taskRow(implicit cache: Cache, messages: Messages): TaskRow = {
    val notStarted = TaskRow(
      key,
      controllers.supervision.routes.WhatYouNeedController.get().url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )
    val respUrl    = controllers.supervision.routes.SummaryController.get().url
    cache.getEntry[Supervision](key).fold(notStarted) {
      case m if m.isComplete && m.hasChanged =>
        TaskRow(
          key,
          controllers.supervision.routes.SummaryController.get().url,
          hasChanged = true,
          status = Updated,
          tag = TaskRow.updatedTag
        )
      case model @ m if m.isComplete         =>
        TaskRow(
          key,
          controllers.routes.YourResponsibilitiesUpdateController.get(respUrl).url,
          model.hasChanged,
          Completed,
          TaskRow.completedTag
        )
      case m if m.isEmpty                    => notStarted
      case model                             =>
        TaskRow(
          key,
          controllers.supervision.routes.WhatYouNeedController.get().url,
          model.hasChanged,
          Started,
          TaskRow.incompleteTag
        )
    }
  }

  import play.api.libs.json._
  import utils.MappingUtils._

  val key = "supervision"

  implicit val mongoKey: MongoKey[Supervision] = () => "supervision"

  def professionalBodiesReader: Reads[Option[ProfessionalBodies]] =
    (__ \ "professionalBodies").readNullable[ProfessionalBodies] flatMap {
      case businessTypes @ Some(_) => constant(businessTypes)
      case _                       => (__ \ "professionalBodyMember").readNullable[ProfessionalBodies] orElse constant(None)
    }

  implicit val reads: Reads[Supervision] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "anotherBody").readNullable[AnotherBody] and
        (__ \ "professionalBodyMember").readNullable[ProfessionalBodyMember] and
        professionalBodiesReader and
        (__ \ "professionalBody").readNullable[ProfessionalBody] and
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
        (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
    ) apply Supervision.apply _
  }

  implicit val writes: Writes[Supervision] = Json.writes[Supervision]

  implicit val formatOption: Reads[Option[Supervision]] = Reads.optionWithNull[Supervision]

  implicit def default(supervision: Option[Supervision]): Supervision =
    supervision.getOrElse(Supervision())
}
