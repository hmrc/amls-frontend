/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.{Reads, Writes}

final case class ApprovalFlags(
                          hasAlreadyPassedFitAndProper: Option[Boolean] = None
//                          hasAlreadyPassedApprovalCheck: Option[Boolean] = None
                        )

object ApprovalFlags {

  implicit lazy val reads: Reads[ApprovalFlags] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "hasAlreadyPassedFitAndProper").readNullable[Boolean].map(ApprovalFlags.apply)
//
//    (
//        (__ \ "hasAlreadyPassedFitAndProper").readNullable[Boolean] and
//        (__ \ "hasAlreadyPassedApprovalCheck").readNullable[Boolean]
//      )(ApprovalFlags.apply _)
  }

  implicit lazy val writes: Writes[ApprovalFlags] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "hasAlreadyPassedFitAndProper").writeNullable[Boolean].contramap(unlift(ApprovalFlags.unapply))
//    (
//        (__ \ "hasAlreadyPassedFitAndProper").writeNullable[Boolean] and
//        (__ \ "hasAlreadyPassedApprovalCheck").writeNullable[Boolean]
//      )(unlift(ApprovalFlags.unapply))
  }
}
