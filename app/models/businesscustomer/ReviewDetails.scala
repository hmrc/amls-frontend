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

import cats._
import cats.implicits._
import connectors.{BusinessMatchingAddress, BusinessMatchingReviewDetails}
import models.Country
import models.businessmatching.BusinessType
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership, SoleProprietor, UnincorporatedBody}
import play.api.Logger
import play.api.libs.json.{Json, Reads}

case class ReviewDetails(
                          businessName: String,
                          businessType: Option[BusinessType],
                          businessAddress: Address,
                          safeId: String,
                          utr: Option[String] = None
                        )

object ReviewDetails {

  private val toBusinessType: PartialFunction[String, BusinessType] = {
    case "Sole Trader" => SoleProprietor
    case "Corporate Body" => LimitedCompany
    case "Partnership" => Partnership
    case "LLP" => LPrLLP
    case "Unincorporated Body" => UnincorporatedBody
  }

  implicit val reads: Reads[ReviewDetails] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (
      (__ \ "businessName").read[String] and
        (__ \ "businessType").readNullable[String].map[Option[BusinessType]] {
          case Some(bt) if toBusinessType.isDefinedAt(bt) => Some(toBusinessType(bt))
          case _ => None
        } and
        (__ \ "businessAddress").read[Address] and
        (__ \ "safeId").read[String] and
        (__ \ "utr").readNullable[String]
      ) (ReviewDetails.apply _)
  }

  implicit val writes = Json.writes[ReviewDetails]

  implicit def convert(addr: BusinessMatchingAddress): Address =
    Address(addr.line_1, addr.line_2, addr.line_3, addr.line_4, addr.postcode, Country("", addr.country))

  implicit def convert(details: BusinessMatchingReviewDetails): ReviewDetails = {
    ReviewDetails(details.businessName, Functor[Option].lift(toBusinessType)(details.businessType), details.businessAddress, details.safeId, details.utr)
  }

}
