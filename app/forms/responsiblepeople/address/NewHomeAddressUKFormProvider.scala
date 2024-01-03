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

package forms.responsiblepeople.address

import forms.generic.AddressFormProvider
import models.responsiblepeople.{NewHomeAddress, PersonAddressUK, ResponsiblePersonCurrentAddress}
import play.api.data.Form

import javax.inject.Inject

class NewHomeAddressUKFormProvider @Inject()() extends AddressFormProvider[NewHomeAddress] {

  override val countryErrorKey: String = ""

  override def toObject: (String, Option[String], Option[String], Option[String], String) => NewHomeAddress = {
    case (line1, line2, line3, line4, postcode) =>
      NewHomeAddress(PersonAddressUK(line1, line2, line3, line4, postcode))
  }

  override def fromObject: NewHomeAddress => Option[(String, Option[String], Option[String], Option[String], String)] = {
    case NewHomeAddress(PersonAddressUK(addressLine1, addressLine2, addressLine3, addressLine4, postCode)) =>
      Some((addressLine1, addressLine2, addressLine3, addressLine4, postCode))
    case _ => None
  }

  def apply(): Form[NewHomeAddress] = createForm(isUKAddress = true)
}