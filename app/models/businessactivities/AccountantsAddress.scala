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

package models.businessactivities

import models.Country
import play.api.libs.json.{Reads, Writes}

sealed trait AccountantsAddress {

  def isUk: Boolean = this match {
    case _: UkAccountantsAddress    => true
    case _: NonUkAccountantsAddress => false
  }

  def isComplete: Boolean = this match {
    case UkAccountantsAddress(al1, _, _, _, ap) if al1.nonEmpty & ap.nonEmpty                         => true
    case NonUkAccountantsAddress(al1, _, _, _, c) if al1.nonEmpty & c.name.nonEmpty & c.code.nonEmpty => true
    case _                                                                                            => false
  }

  def toLines: Seq[String] = this match {
    case a: UkAccountantsAddress    =>
      Seq(
        Some(a.addressLine1),
        a.addressLine2,
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: NonUkAccountantsAddress =>
      Seq(
        Some(a.addressLine1),
        a.addressLine2,
        a.addressLine3,
        a.addressLine4,
        Some(a.country.toString)
      ).flatten
  }
}

case class UkAccountantsAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postCode: String
) extends AccountantsAddress

case class NonUkAccountantsAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  country: Country
) extends AccountantsAddress

object AccountantsAddress {

  implicit val jsonReads: Reads[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "accountantsAddressPostCode").read[String] andKeep (
      ((__ \ "accountantsAddressLine1").read[String] and
        (__ \ "accountantsAddressLine2").readNullable[String] and
        (__ \ "accountantsAddressLine3").readNullable[String] and
        (__ \ "accountantsAddressLine4").readNullable[String] and
        (__ \ "accountantsAddressPostCode").read[String])(UkAccountantsAddress.apply _) map identity[AccountantsAddress]
    ) orElse
      ((__ \ "accountantsAddressLine1").read[String] and
        (__ \ "accountantsAddressLine2").readNullable[String] and
        (__ \ "accountantsAddressLine3").readNullable[String] and
        (__ \ "accountantsAddressLine4").readNullable[String] and
        (__ \ "accountantsAddressCountry").read[Country])(NonUkAccountantsAddress.apply _)

  }

  implicit val jsonWrites: Writes[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[AccountantsAddress] {
      case a: UkAccountantsAddress    =>
        (
          (__ \ "accountantsAddressLine1").write[String] and
            (__ \ "accountantsAddressLine2").writeNullable[String] and
            (__ \ "accountantsAddressLine3").writeNullable[String] and
            (__ \ "accountantsAddressLine4").writeNullable[String] and
            (__ \ "accountantsAddressPostCode").write[String]
        )(unlift(UkAccountantsAddress.unapply)).writes(a)
      case a: NonUkAccountantsAddress =>
        (
          (__ \ "accountantsAddressLine1").write[String] and
            (__ \ "accountantsAddressLine2").writeNullable[String] and
            (__ \ "accountantsAddressLine3").writeNullable[String] and
            (__ \ "accountantsAddressLine4").writeNullable[String] and
            (__ \ "accountantsAddressCountry").write[Country]
        )(unlift(NonUkAccountantsAddress.unapply)).writes(a)
    }
  }
}
