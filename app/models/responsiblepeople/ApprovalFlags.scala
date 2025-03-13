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

package models.responsiblepeople

import play.api.libs.json.{Json, OFormat, Reads, Writes}

final case class ApprovalFlags(
  hasAlreadyPassedFitAndProper: Option[Boolean] = None,
  hasAlreadyPaidApprovalCheck: Option[Boolean] = None
) {
  def isComplete() =
    hasAlreadyPassedFitAndProper.isDefined &
      hasAlreadyPaidApprovalCheck.isDefined

}

object ApprovalFlags {

  implicit lazy val reads: Reads[ApprovalFlags] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "hasAlreadyPassedFitAndProper").readNullable[Boolean] and
        (__ \ "hasAlreadyPaidApprovalCheck").readNullable[Boolean]
    )(ApprovalFlags.apply _)
  }

  implicit lazy val writes: Writes[ApprovalFlags] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "hasAlreadyPassedFitAndProper").writeNullable[Boolean] and
        (__ \ "hasAlreadyPaidApprovalCheck").writeNullable[Boolean]
    )(unlift(ApprovalFlags.unapply))
  }

  implicit val format: OFormat[ApprovalFlags] = Json.format[ApprovalFlags]
}
