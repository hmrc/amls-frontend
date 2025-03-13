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

package forms.businessactivities

import forms.generic.AddressFormProvider
import models.businessactivities.{AccountantsAddress, UkAccountantsAddress}
import play.api.data.Form

import javax.inject.Inject

class AccountantUKAddressFormProvider @Inject() () extends AddressFormProvider[AccountantsAddress] {

  override val countryErrorKey: String = ""

  override def toObject: (String, Option[String], Option[String], Option[String], String) => AccountantsAddress = {
    case (line1, line2, line3, line4, postcode) => UkAccountantsAddress(line1, line2, line3, line4, postcode)
  }

  override def fromObject
    : AccountantsAddress => Option[(String, Option[String], Option[String], Option[String], String)] = {
    case UkAccountantsAddress(addressLine1, addressLine2, addressLine3, addressLine4, postCode) =>
      Some((addressLine1, addressLine2, addressLine3, addressLine4, postCode))
    case _                                                                                      => None
  }

  def apply(): Form[AccountantsAddress] = createForm(isUKAddress = true)

}
