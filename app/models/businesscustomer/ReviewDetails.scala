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

package models.businesscustomer

import models.businessmatching.BusinessType
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership, SoleProprietor, UnincorporatedBody}
import play.api.Logger
import play.api.libs.json.{Json, Reads, Writes}

case class ReviewDetails(
                          businessName: String,
                          businessType: Option[BusinessType],
                          businessAddress: Address,
                          //                        sapNumber: String,
                          safeId: String
                          //                        isAGroup: Boolean,
                          //                        directMatch: Boolean,
                          //                        agentReferenceNumber: Option[String],
                          //                        firstName: Option[String],
                          //                        lastName: Option[String]
                        )

object ReviewDetails {

  implicit val reads: Reads[ReviewDetails] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (
      (__ \ "businessName").read[String] and
        (__ \ "businessType").readNullable[String].map[Option[BusinessType]] {
          typeOption =>
            Logger.debug(s"[ReviewDetails - BusinessType: $typeOption}")
            typeOption match {
              case Some("Sole Trader") => Some(SoleProprietor)
              case Some("Corporate Body") => Some(LimitedCompany)
              case Some("Partnership") => Some(Partnership)
              case Some("LLP") => Some(LPrLLP)
              case Some("Unincorporated Body") => Some(UnincorporatedBody)
              case _ => None
            }
        } and
        (__ \ "businessAddress").read[Address] and
        (__ \ "safeId").read[String]
      ) (ReviewDetails.apply _)
  }

  implicit val writes = Json.writes[ReviewDetails]
}
