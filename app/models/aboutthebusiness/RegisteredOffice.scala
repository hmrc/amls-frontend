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

package models.aboutthebusiness

import models.{Country, DateOfChange}
import models.FormTypes._
import models.businesscustomer.Address
import org.joda.time.LocalDate
import jto.validation._
import jto.validation.forms._
import play.api.libs.json.{JsNull, Json, Reads, Writes}

sealed trait RegisteredOffice {

  def toLines: Seq[String] = this match {
    case a: RegisteredOfficeUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: RegisteredOfficeNonUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.country.toString)
      ).flatten
  }

  def dateOfChange: Option[DateOfChange]
}

case class RegisteredOfficeUK(
                               addressLine1: String,
                               addressLine2: String,
                               addressLine3: Option[String] = None,
                               addressLine4: Option[String] = None,
                               postCode: String,
                               dateOfChange: Option[DateOfChange] = None
                             ) extends RegisteredOffice

case class RegisteredOfficeNonUK(
                                  addressLine1: String,
                                  addressLine2: String,
                                  addressLine3: Option[String] = None,
                                  addressLine4: Option[String] = None,
                                  country: Country,
                                  dateOfChange: Option[DateOfChange] = None
                                ) extends RegisteredOffice

object RegisteredOffice {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, RegisteredOffice] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "isUK").read[Boolean].withMessage("error.required.atb.registered.office.uk.or.overseas") flatMap {
      case true =>
        (
          (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") andThen validateAddress) ~
            (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") andThen validateAddress) ~
            (__ \ "addressLine3").read(optionR(validateAddress)) ~
            (__ \ "addressLine4").read(optionR(validateAddress)) ~
            (__ \ "postCode").read(notEmptyStrip andThen postcodeType)
          ) ((addr1: String, addr2: String, addr3: Option[String], addr4: Option[String], postCode: String) =>
            RegisteredOfficeUK(addr1, addr2, addr3, addr4, postCode, None))

      case false =>
        (
          (__ \ "addressLineNonUK1").read(notEmpty.withMessage("error.required.address.line1") andThen validateAddress) ~
            (__ \ "addressLineNonUK2").read(notEmpty.withMessage("error.required.address.line2") andThen validateAddress) ~
            (__ \ "addressLineNonUK3").read(optionR(validateAddress)) ~
            (__ \ "addressLineNonUK4").read(optionR(validateAddress)) ~
            (__ \ "country").read[Country]
          ) ((addr1: String, addr2: String, addr3: Option[String], addr4: Option[String], country: Country) =>
          RegisteredOfficeNonUK(addr1, addr2, addr3, addr4, country, None))
    }
  }

  implicit val formWrites: Write[RegisteredOffice, UrlFormEncoded] = Write {
    case f: RegisteredOfficeUK =>
      Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> f.addressLine1,
        "addressLine2" -> f.addressLine2,
        "addressLine3" -> Seq(f.addressLine3.getOrElse("")),
        "addressLine4" -> Seq(f.addressLine4.getOrElse("")),
        "postCode" -> f.postCode
      )
    case f: RegisteredOfficeNonUK =>
      Map(
        "isUK" -> Seq("false"),
        "addressLineNonUK1" -> f.addressLine1,
        "addressLineNonUK2" -> f.addressLine2,
        "addressLineNonUK3" -> Seq(f.addressLine3.getOrElse("")),
        "addressLineNonUK4" -> Seq(f.addressLine4.getOrElse("")),
        "country" -> f.country.code
      )
  }

  implicit val jsonReads: Reads[RegisteredOffice] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (
      (__ \ "postCode").read[String] andKeep
        (
          (__ \ "addressLine1").read[String] and
            (__ \ "addressLine2").read[String] and
            (__ \ "addressLine3").readNullable[String] and
            (__ \ "addressLine4").readNullable[String] and
            (__ \ "postCode").read[String] and
            (__ \ "dateOfChange").readNullable[DateOfChange]
          ) (RegisteredOfficeUK.apply _) map identity[RegisteredOffice]
      ) orElse
      (
        (__ \ "addressLineNonUK1").read[String] and
          (__ \ "addressLineNonUK2").read[String] and
          (__ \ "addressLineNonUK3").readNullable[String] and
          (__ \ "addressLineNonUK4").readNullable[String] and
          (__ \ "country").read[Country] and
          (__ \ "dateOfChange").readNullable[DateOfChange]
        ) (RegisteredOfficeNonUK.apply _)
  }

  implicit val jsonWrites = Writes[RegisteredOffice] {

    case m: RegisteredOfficeUK =>
      Json.obj(
        "addressLine1" -> m.addressLine1,
        "addressLine2" -> m.addressLine2,
        "addressLine3" -> m.addressLine3,
        "addressLine4" -> m.addressLine4,
        "postCode" -> m.postCode,
        "dateOfChange" -> m.dateOfChange
      )

    case m: RegisteredOfficeNonUK =>
      Json.obj(
        "addressLineNonUK1" -> m.addressLine1,
        "addressLineNonUK2" -> m.addressLine2,
        "addressLineNonUK3" -> m.addressLine3,
        "addressLineNonUK4" -> m.addressLine4,
        "country" -> m.country.code,
        "dateOfChange" -> m.dateOfChange
      )
  }

  implicit def convert(address: Address): RegisteredOffice = {
    address.postcode match {
      case Some(data) => RegisteredOfficeUK(address.line_1,
        address.line_2,
        address.line_3,
        address.line_4,
        data)
      case None => RegisteredOfficeNonUK(address.line_1,
        address.line_2,
        address.line_3,
        address.line_4,
        address.country)
    }
  }
}
