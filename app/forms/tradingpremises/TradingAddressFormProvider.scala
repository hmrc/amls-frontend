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

package forms.tradingpremises

import forms.mappings.AddressMappings
import models.tradingpremises.{Address, YourTradingPremises}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class TradingAddressFormProvider @Inject() () extends AddressMappings {

  override val countryErrorKey: String = ""
  val tradingNameLength                = 120

  def apply(): Form[YourTradingPremises] = Form[YourTradingPremises](
    mapping(
      "tradingName"  -> text("error.required.tp.trading.name").verifying(
        firstError(
          maxLength(tradingNameLength, "error.invalid.tp.trading.name"),
          regexp(basicPunctuationRegex, "error.invalid.char.tp.agent.company.details")
        )
      ),
      "addressLine1" -> addressLineMapping("line1"),
      "addressLine2" -> optional(addressLineMapping("line2")),
      "addressLine3" -> optional(addressLineMapping("line3")),
      "addressLine4" -> optional(addressLineMapping("line4")),
      postcodeOrCountryMapping(true)
    )(toObject)(fromObject)
  )

  private def toObject
    : (String, String, Option[String], Option[String], Option[String], String) => YourTradingPremises = {
    case (name, line1, line2, line3, line4, postcode) =>
      YourTradingPremises(name, Address(line1, line2, line3, line4, postcode))
  }

  private def fromObject
    : YourTradingPremises => Option[(String, String, Option[String], Option[String], Option[String], String)] = {
    case YourTradingPremises(
          name,
          Address(addressLine1, addressLine2, addressLine3, addressLine4, postcode, _),
          _,
          _,
          _
        ) =>
      Some((name, addressLine1, addressLine2, addressLine3, addressLine4, postcode))
    case _ => None
  }
}
