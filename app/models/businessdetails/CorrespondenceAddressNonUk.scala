/*
 * Copyright 2019 HM Revenue & Customs
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

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, ValidationError, Write}
import models.Country

case class CorrespondenceAddressNonUk(
                                     yourName: String,
                                     businessName: String,
                                     addressLineNonUK1: String,
                                     addressLineNonUK2: String,
                                     addressLineNonUK3: Option[String],
                                     addressLineNonUK4: Option[String],
                                     country: Country
                                     ) {

  def toLines: Seq[String] =
    Seq(
      Some(yourName),
      Some(businessName),
      Some(addressLineNonUK1),
      Some(addressLineNonUK2),
      addressLineNonUK3,
      addressLineNonUK4,
      Some(country.toString)
    ).flatten
}

object CorrespondenceAddressNonUk {
  implicit val formRule: Rule[UrlFormEncoded, CorrespondenceAddressNonUk] = From[UrlFormEncoded] { __ =>

      val validateCountry: Rule[Country, Country] = Rule.fromMapping[Country, Country] { country =>
        country.code match {
          case "GB" => Invalid(Seq(ValidationError(List("error.required.atb.letters.address.not.uk"))))
          case _ => Valid(country)
        }
      }
      import jto.validation.forms.Rules._
      import models.FormTypes._
      import utils.MappingUtils.Implicits._

      val nameMaxLength = 140
      val businessNameMaxLength = 120

      val alternativeAddressNameType = notEmptyStrip andThen
        nameRequired andThen
        maxLength(nameMaxLength).withMessage("error.invalid.yourname") andThen
        basicPunctuationPattern()

      val alternativeAddressTradingNameType = notEmptyStrip andThen
        required("error.required.name.of.business") andThen
        maxLength(businessNameMaxLength).withMessage("error.invalid.name.of.business") andThen
        basicPunctuationPattern()
     (
            (__ \ "yourName").read(alternativeAddressNameType) ~
            (__ \ "businessName").read(alternativeAddressTradingNameType) ~
            (__ \ "addressLineNonUK1").read(notEmpty.withMessage("error.required.address.line1") andThen validateAddress) ~
            (__ \ "addressLineNonUK2").read(notEmpty.withMessage("error.required.address.line2") andThen validateAddress) ~
            (__ \ "addressLineNonUK3").read(optionR(validateAddress)) ~
            (__ \ "addressLineNonUK4").read(optionR(validateAddress)) ~
            (__ \ "country").read(validateCountry)
          )(CorrespondenceAddressNonUk.apply _)
    }

  implicit val formWrites = Write[CorrespondenceAddressNonUk, UrlFormEncoded] {
    a: CorrespondenceAddressNonUk =>
      Map(
        "yourName" -> Seq(a.yourName),
        "businessName" -> Seq(a.businessName),
        "addressLineNonUK1" -> Seq(a.addressLineNonUK1),
        "addressLineNonUK2" -> Seq(a.addressLineNonUK2),
        "addressLineNonUK3" -> a.addressLineNonUK3.toSeq,
        "addressLineNonUK4" -> a.addressLineNonUK4.toSeq,
        "country" -> Seq(a.country.code)
      )
  }
}

