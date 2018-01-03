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

package models.tradingpremises

import models.DateOfChange
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, To, Write}
import play.api.libs.json.{Json, Reads, Writes}

case class Address(
                  addressLine1: String,
                  addressLine2: String,
                  addressLine3: Option[String],
                  addressLine4: Option[String],
                  postcode: String,
                  dateOfChange: Option[DateOfChange] = None
                  ) {

  def toLines: Seq[String] = Seq(
    Some(addressLine1),
    Some(addressLine2),
    addressLine3,
    addressLine4,
    Some(postcode)
  ).flatten
}

object Address {
  import utils.MappingUtils.Implicits._

  def applyWithoutDateOfChange(address1: String, address2: String, address3: Option[String], address4: Option[String], postcode: String) =
    Address(address1, address2, address3, address4, postcode)

  def unapplyWithoutDateOfChange(x: Address) =
    Some((x.addressLine1, x.addressLine2, x.addressLine3, x.addressLine4, x.postcode))

  implicit val formR: Rule[UrlFormEncoded, Address] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import jto.validation.forms.Rules._
      (
        (__ \ "addressLine1").read(notEmptyStrip andThen notEmpty.withMessage("error.required.address.line1") andThen validateAddress) ~
          (__ \ "addressLine2").read(notEmptyStrip andThen notEmpty.withMessage("error.required.address.line2") andThen validateAddress) ~
          (__ \ "addressLine3").read(optionR(notEmptyStrip andThen validateAddress)) ~
          (__ \ "addressLine4").read(optionR(notEmptyStrip andThen validateAddress)) ~
          (__ \ "postcode").read(notEmptyStrip andThen postcodeType)
        )(Address.applyWithoutDateOfChange)
    }

  implicit val formW: Write[Address, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      import jto.validation.forms.Writes._
      import play.api.libs.functional.syntax.unlift
      (
        (__ \ "addressLine1").write[String] ~
          (__ \ "addressLine2").write[String] ~
          (__ \ "addressLine3").write[Option[String]] ~
          (__ \ "addressLine4").write[Option[String]] ~
          (__ \ "postcode").write[String]
        )(unlift(Address.unapplyWithoutDateOfChange))
    }

  implicit val reads: Reads[Address] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "addressLine1").read[String] and
        (__ \ "addressLine2").read[String] and
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
        (__ \ "addressLine2").write[String] and
        (__ \ "addressLine3").writeNullable[String] and
        (__ \ "addressLine4").writeNullable[String] and
        (__ \ "postcode").write[String] and
        (__ \ "addressDateOfChange").writeNullable[DateOfChange]
      )(unlift(Address.unapply))
  }
}
