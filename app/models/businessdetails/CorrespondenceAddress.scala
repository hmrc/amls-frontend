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

package models.businessdetails

import models.Country
import play.api.libs.json.{Reads, Writes}

case class CorrespondenceAddress(
  ukAddress: Option[CorrespondenceAddressUk],
  nonUkAddress: Option[CorrespondenceAddressNonUk]
) {

  def isUk: Option[Boolean] =
    this match {
      case CorrespondenceAddress(Some(_), None) => Some(true)
      case CorrespondenceAddress(None, Some(_)) => Some(false)
      case _                                    => None
    }
}

object CorrespondenceAddress {

  implicit val jsonReads: Reads[CorrespondenceAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "correspondencePostCode").readNullable[String] andKeep
      (
        ((__ \ "yourName").read[String] and
          (__ \ "businessName").read[String] and
          (__ \ "correspondenceAddressLine1").read[String] and
          (__ \ "correspondenceAddressLine2").readNullable[String] and
          (__ \ "correspondenceAddressLine3").readNullable[String] and
          (__ \ "correspondenceAddressLine4").readNullable[String] and
          (__ \ "correspondencePostCode").read[String])(CorrespondenceAddressUk.apply _)
          .map(x => CorrespondenceAddress(Some(x), None))
          orElse
            ((__ \ "yourName").read[String] and
              (__ \ "businessName").read[String] and
              (__ \ "correspondenceAddressLine1").read[String] and
              (__ \ "correspondenceAddressLine2").readNullable[String] and
              (__ \ "correspondenceAddressLine3").readNullable[String] and
              (__ \ "correspondenceAddressLine4").readNullable[String] and
              (__ \ "correspondenceCountry").read[Country])(CorrespondenceAddressNonUk.apply _)
              .map(x => CorrespondenceAddress(None, Some(x)))
      )
  }

  implicit val jsonWrites: Writes[CorrespondenceAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[CorrespondenceAddress] {
      case CorrespondenceAddress(Some(a), None) =>
        (
          (__ \ "yourName").write[String] and
            (__ \ "businessName").write[String] and
            (__ \ "correspondenceAddressLine1").write[String] and
            (__ \ "correspondenceAddressLine2").writeNullable[String] and
            (__ \ "correspondenceAddressLine3").writeNullable[String] and
            (__ \ "correspondenceAddressLine4").writeNullable[String] and
            (__ \ "correspondencePostCode").write[String]
        )(unlift(CorrespondenceAddressUk.unapply)).writes(a)
      case CorrespondenceAddress(None, Some(a)) =>
        (
          (__ \ "yourName").write[String] and
            (__ \ "businessName").write[String] and
            (__ \ "correspondenceAddressLine1").write[String] and
            (__ \ "correspondenceAddressLine2").writeNullable[String] and
            (__ \ "correspondenceAddressLine3").writeNullable[String] and
            (__ \ "correspondenceAddressLine4").writeNullable[String] and
            (__ \ "correspondenceCountry").write[Country]
        )(unlift(CorrespondenceAddressNonUk.unapply)).writes(a)
      case _                                    => throw new Exception("An UnknownException has occurred while parsing CorrespondenceAddress")
    }
  }
}
