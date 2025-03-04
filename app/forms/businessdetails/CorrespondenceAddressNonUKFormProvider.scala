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

package forms.businessdetails

import forms.generic.CorrespondenceAddressFormProvider
import models.businessdetails.CorrespondenceAddressNonUk
import play.api.data.Form

class CorrespondenceAddressNonUKFormProvider extends CorrespondenceAddressFormProvider[CorrespondenceAddressNonUk] {

  override val countryErrorKey: String = "error.required.atb.letters.address.not.uk"

  override def toObject
    : (String, String, String, Option[String], Option[String], Option[String], String) => CorrespondenceAddressNonUk = {
    case (yourName, businessName, line1, line2, line3, line4, country) =>
      CorrespondenceAddressNonUk(yourName, businessName, line1, line2, line3, line4, parseCountry(country))
  }

  override def fromObject: CorrespondenceAddressNonUk => Option[
    (String, String, String, Option[String], Option[String], Option[String], String)
  ] = {
    case CorrespondenceAddressNonUk(name, business, addressLine1, addressLine2, addressLine3, addressLine4, country) =>
      Some((name, business, addressLine1, addressLine2, addressLine3, addressLine4, country.code))
    case _                                                                                                           => None
  }

  def apply(): Form[CorrespondenceAddressNonUk] = createForm(false)
}
