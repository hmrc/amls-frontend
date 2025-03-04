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

package models.tradingpremises

import models.{Country, DateOfChange}
import play.api.libs.json.{Reads, Writes}
import models.businesscustomer.{Address => BCAddress}

case class Address(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postcode: String,
  dateOfChange: Option[DateOfChange] = None
) {

  def toLines: Seq[String] = Seq(
    Some(addressLine1),
    addressLine2,
    addressLine3,
    addressLine4,
    Some(postcode)
  ).flatten

  def toBCAddress: BCAddress =
    BCAddress(addressLine1, addressLine2, addressLine3, addressLine4, Some(postcode), Country("", ""))
}

object Address {

  def applyWithoutDateOfChange(
    address1: String,
    address2: Option[String],
    address3: Option[String],
    address4: Option[String],
    postcode: String
  ) =
    Address(address1, address2, address3, address4, postcode)

  def unapplyWithoutDateOfChange(x: Address) =
    Some((x.addressLine1, x.addressLine2, x.addressLine3, x.addressLine4, x.postcode))

  implicit val reads: Reads[Address] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "addressLine1").read[String] and
        (__ \ "addressLine2").readNullable[String] and
        (__ \ "addressLine3").readNullable[String] and
        (__ \ "addressLine4").readNullable[String] and
        (__ \ "postcode").read[String] and
        (__ \ "addressDateOfChange").readNullable[DateOfChange]
    )(Address.apply _)
  }

  implicit val writes: Writes[Address] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "addressLine1").write[String] and
        (__ \ "addressLine2").writeNullable[String] and
        (__ \ "addressLine3").writeNullable[String] and
        (__ \ "addressLine4").writeNullable[String] and
        (__ \ "postcode").write[String] and
        (__ \ "addressDateOfChange").writeNullable[DateOfChange]
    )(unlift(Address.unapply))
  }
}
