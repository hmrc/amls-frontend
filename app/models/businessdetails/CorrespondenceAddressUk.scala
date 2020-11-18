/*
 * Copyright 2020 HM Revenue & Customs
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

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}

case class CorrespondenceAddressUk(
                                  yourName: String,
                                  businessName: String,
                                  addressLine1: String,
                                  addressLine2: String,
                                  addressLine3: Option[String],
                                  addressLine4: Option[String],
                                  postCode: String
                                  ) {
  def toLines: Seq[String] =
      Seq(
        Some(yourName),
        Some(businessName),
        Some(addressLine1),
        Some(addressLine2),
        addressLine3,
        addressLine4,
        Some(postCode)
      ).flatten
}

object CorrespondenceAddressUk {

  implicit val formRule: Rule[UrlFormEncoded, CorrespondenceAddressUk] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._
    import models.FormTypes._
    import utils.MappingUtils.Implicits._

    val nameMaxLength = 140
    val businessNameMaxLength = 120

    val alternativeAddressNameType = notEmptyStrip andThen
      nameRequired andThen
      maxLength(nameMaxLength).withMessage("error.invalid.yourname") andThen
      basicPunctuationPattern("error.invalid.yourname.validation")

    val alternativeAddressTradingNameType = notEmptyStrip andThen
      required("error.required.name.of.business") andThen
      maxLength(businessNameMaxLength).withMessage("error.invalid.name.of.business") andThen
      basicPunctuationPattern("error.invalid.name.of.business.validation")

    ((__ \ "yourName").read(alternativeAddressNameType) ~
      (__ \ "businessName").read(alternativeAddressTradingNameType) ~
      (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") andThen validateAddress("line1")) ~
      (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") andThen validateAddress("line2")) ~
      (__ \ "addressLine3").read(optionR(validateAddress("line3"))) ~
      (__ \ "addressLine4").read(optionR(validateAddress("line4"))) ~
      (__ \ "postCode").read(notEmptyStrip andThen postcodeType)
    )(CorrespondenceAddressUk.apply _)
  }

  implicit val formWrites = Write[CorrespondenceAddressUk, UrlFormEncoded] {
    a => Map(
        "yourName" -> Seq(a.yourName),
        "businessName" -> Seq(a.businessName),
        "addressLine1" -> Seq(a.addressLine1),
        "addressLine2" -> Seq(a.addressLine2),
        "addressLine3" -> a.addressLine3.toSeq,
        "addressLine4" -> a.addressLine4.toSeq,
        "postCode" -> Seq(a.postCode)
      )
  }
}

