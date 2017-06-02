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

package models.responsiblepeople

import models.Country
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Path, From, Rule, Write}
import play.api.libs.json.{Reads, Writes}

sealed trait PersonAddress {

  def toLines: Seq[String] = this match {
    case a: PersonAddressUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: PersonAddressNonUK =>
      Seq(
        Some(a.addressLineNonUK1),
        Some(a.addressLineNonUK2),
        a.addressLineNonUK3,
        a.addressLineNonUK4,
        Some(a.country.toString)
      ).flatten
  }
}

case class PersonAddressUK(
                      addressLine1: String,
                      addressLine2: String,
                      addressLine3: Option[String],
                      addressLine4: Option[String],
                      postCode: String) extends PersonAddress

case class PersonAddressNonUK(
                         addressLineNonUK1: String,
                         addressLineNonUK2: String,
                         addressLineNonUK3: Option[String],
                         addressLineNonUK4: Option[String],
                         country: Country) extends PersonAddress

object PersonAddress {
  implicit val formRule: Rule[UrlFormEncoded, PersonAddress] = From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      import models.FormTypes._
      import utils.MappingUtils.Implicits._

      (__ \ "isUK").read[Boolean].withMessage("error.required.uk.or.overseas") flatMap {
        case true => (
            (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") andThen validateAddress) ~
            (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") andThen validateAddress) ~
            (__ \ "addressLine3").read(optionR(validateAddress)) ~
            (__ \ "addressLine4").read(optionR(validateAddress)) ~
            (__ \ "postCode").read(notEmptyStrip andThen postcodeType)
          )(PersonAddressUK.apply _)
        case false => (
            (__ \ "addressLineNonUK1").read(notEmpty.withMessage("error.required.address.line1") andThen validateAddress) ~
            (__ \ "addressLineNonUK2").read(notEmpty.withMessage("error.required.address.line2") andThen validateAddress) ~
            (__ \ "addressLineNonUK3").read(optionR(validateAddress)) ~
            (__ \ "addressLineNonUK4").read(optionR(validateAddress)) ~
            (__ \ "country").read[Country]
          )(PersonAddressNonUK.apply _)
      }
    }

  implicit val formWrites = Write[PersonAddress, UrlFormEncoded] {
    case a: PersonAddressUK =>
      Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq(a.addressLine1),
        "addressLine2" -> Seq(a.addressLine2),
        "addressLine3" -> a.addressLine3.toSeq,
        "addressLine4" -> a.addressLine4.toSeq,
        "postCode" -> Seq(a.postCode)
      )
    case a: PersonAddressNonUK =>
      Map(
        "isUK" -> Seq("false"),
        "addressLineNonUK1" -> Seq(a.addressLineNonUK1),
        "addressLineNonUK2" -> Seq(a.addressLineNonUK2),
        "addressLineNonUK3" -> a.addressLineNonUK3.toSeq,
        "addressLineNonUK4" -> a.addressLineNonUK4.toSeq,
        "country" -> Seq(a.country.code)
      )
  }

  implicit val jsonReads: Reads[PersonAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "personAddressPostCode").read[String] andKeep (
      (
        (__ \ "personAddressLine1").read[String] and
        (__ \ "personAddressLine2").read[String] and
        (__ \ "personAddressLine3").readNullable[String] and
        (__ \ "personAddressLine4").readNullable[String] and
        (__ \ "personAddressPostCode").read[String])(PersonAddressUK.apply _) map identity[PersonAddress]
      ) orElse
      (
        (__ \ "personAddressLine1").read[String] and
        (__ \ "personAddressLine2").read[String] and
        (__ \ "personAddressLine3").readNullable[String] and
        (__ \ "personAddressLine4").readNullable[String] and
        (__ \ "personAddressCountry").read[Country])(PersonAddressNonUK.apply _)
  }

  implicit val jsonWrites: Writes[PersonAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[PersonAddress] {
      case a: PersonAddressUK =>
        (
            (__ \ "personAddressLine1").write[String] and
            (__ \ "personAddressLine2").write[String] and
            (__ \ "personAddressLine3").writeNullable[String] and
            (__ \ "personAddressLine4").writeNullable[String] and
            (__ \ "personAddressPostCode").write[String]
          )(unlift(PersonAddressUK.unapply)).writes(a)
      case a: PersonAddressNonUK =>
        (
            (__ \ "personAddressLine1").write[String] and
            (__ \ "personAddressLine2").write[String] and
            (__ \ "personAddressLine3").writeNullable[String] and
            (__ \ "personAddressLine4").writeNullable[String] and
            (__ \ "personAddressCountry").write[Country]
          )(unlift(PersonAddressNonUK.unapply)).writes(a)
    }
  }
}
