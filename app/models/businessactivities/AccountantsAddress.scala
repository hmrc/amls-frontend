/*
 * Copyright 2023 HM Revenue & Customs
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

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import models.Country
import models.FormTypes._
import play.api.libs.json.{Reads, Writes}

sealed trait AccountantsAddress {

  def isUk: Boolean = this match {
    case _: UkAccountantsAddress => true
    case _: NonUkAccountantsAddress => false
  }

  def isComplete: Boolean= this match {
    case UkAccountantsAddress(al1, al2, _, _, ap) if al1.nonEmpty & al2.nonEmpty & ap.nonEmpty => true
    case NonUkAccountantsAddress(al1, al2, _, _, c) if al1.nonEmpty & al2.nonEmpty & c.name.nonEmpty & c.code.nonEmpty => true
    case _ => false
  }

  def toLines: Seq[String] = this match {
    case a: UkAccountantsAddress =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: NonUkAccountantsAddress =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.country.toString)
      ).flatten
  }
}

case class UkAccountantsAddress(
                                 addressLine1: String,
                                 addressLine2: String,
                                 addressLine3: Option[String],
                                 addressLine4: Option[String],
                                 postCode: String
                               ) extends AccountantsAddress

case class NonUkAccountantsAddress(
                                    addressLine1: String,
                                    addressLine2: String,
                                    addressLine3: Option[String],
                                    addressLine4: Option[String],
                                    country: Country
                                  ) extends AccountantsAddress

object AccountantsAddress {

  val addressLine1Rule = genericAddressRule("error.required.address.line1",
    "error.required.enter.addresslineone.charcount",
    "error.text.validation.address.line1")

  val addressLine2Rule = genericAddressRule("error.required.address.line2",
    "error.required.enter.addresslinetwo.charcount",
    "error.text.validation.address.line2")

  val addressLine3Rule = genericAddressRule("",
    "error.required.enter.addresslinethree.charcount",
    "error.text.validation.address.line3")

  val addressLine4Rule = genericAddressRule("",
    "error.required.enter.addresslinefour.charcount",
    "error.text.validation.address.line4")

  implicit val ukFormRule: Rule[UrlFormEncoded, UkAccountantsAddress] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
      (
        (__ \ "addressLine1").read(addressLine1Rule) ~
        (__ \ "addressLine2").read(addressLine2Rule) ~
        (__ \ "addressLine3").read(optionR(addressLine3Rule)) ~
        (__ \ "addressLine4").read(optionR(addressLine4Rule)) ~
        (__ \ "postCode").read(notEmptyStrip andThen postcodeType)
      ) (UkAccountantsAddress.apply)
  }

  implicit val nonUkFormRule: Rule[UrlFormEncoded, NonUkAccountantsAddress] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
      (
        (__ \ "addressLineNonUK1").read(addressLine1Rule) ~
        (__ \ "addressLineNonUK2").read(addressLine2Rule) ~
        (__ \ "addressLineNonUK3").read(optionR(addressLine3Rule)) ~
        (__ \ "addressLineNonUK4").read(optionR(addressLine4Rule)) ~
        (__ \ "country").read[Country]
      ) (NonUkAccountantsAddress.apply)
  }

  implicit val formWrites = Write[AccountantsAddress, UrlFormEncoded] {
        case address: UkAccountantsAddress => Map(
          "addressLine1" -> Seq(address.addressLine1),
          "addressLine2" -> Seq(address.addressLine2),
          "addressLine3" -> address.addressLine3.toSeq,
          "addressLine4" -> address.addressLine4.toSeq,
          "postCode" -> Seq(address.postCode)
        )
        case address: NonUkAccountantsAddress => Map(
          "addressLineNonUK1" -> Seq(address.addressLine1),
          "addressLineNonUK2" -> Seq(address.addressLine2),
          "addressLineNonUK3" -> address.addressLine3.toSeq,
          "addressLineNonUK4" -> address.addressLine4.toSeq,
          "country" -> Seq(address.country.code)
        )
  }

  implicit val jsonReads: Reads[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "accountantsAddressPostCode").read[String] andKeep (
        ((__ \ "accountantsAddressLine1").read[String] and
        (__ \ "accountantsAddressLine2").read[String] and
        (__ \ "accountantsAddressLine3").readNullable[String] and
        (__ \ "accountantsAddressLine4").readNullable[String] and
        (__ \ "accountantsAddressPostCode").read[String])  (UkAccountantsAddress.apply _) map identity[AccountantsAddress]
      ) orElse
        ( (__ \ "accountantsAddressLine1").read[String] and
          (__ \ "accountantsAddressLine2").read[String] and
          (__ \ "accountantsAddressLine3").readNullable[String] and
          (__ \ "accountantsAddressLine4").readNullable[String] and
          (__ \ "accountantsAddressCountry").read[Country]) (NonUkAccountantsAddress.apply _)

  }

  implicit val jsonWrites: Writes[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[AccountantsAddress] {
      case a: UkAccountantsAddress =>
        (
            (__ \ "accountantsAddressLine1").write[String] and
            (__ \ "accountantsAddressLine2").write[String] and
            (__ \ "accountantsAddressLine3").writeNullable[String] and
            (__ \ "accountantsAddressLine4").writeNullable[String] and
            (__ \ "accountantsAddressPostCode").write[String]
          ) (unlift(UkAccountantsAddress.unapply)).writes(a)
      case a: NonUkAccountantsAddress =>
        (
            (__ \ "accountantsAddressLine1").write[String] and
            (__ \ "accountantsAddressLine2").write[String] and
            (__ \ "accountantsAddressLine3").writeNullable[String] and
            (__ \ "accountantsAddressLine4").writeNullable[String] and
            (__ \ "accountantsAddressCountry").write[Country]
          ) (unlift(NonUkAccountantsAddress.unapply)).writes(a)
    }
  }
}
