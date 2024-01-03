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
import models.responsiblepeople.{PersonAddressNonUK, ResponsiblePersonAddress}
import play.api.data.Form

import javax.inject.Inject

class AdditionalAddressNonUKFormProvider @Inject()() extends AddressFormProvider[ResponsiblePersonAddress]  {

  override val countryErrorKey: String = "error.required.enter.non.uk.previous"

  override def toObject: (String, Option[String], Option[String], Option[String], String) => ResponsiblePersonAddress = {
    case (line1, line2, line3, line4, code) =>
      ResponsiblePersonAddress(PersonAddressNonUK(line1, line2, line3, line4, parseCountry(code)), None)
  }

  override def fromObject: ResponsiblePersonAddress => Option[(String, Option[String], Option[String], Option[String], String)] = {
    case ResponsiblePersonAddress(PersonAddressNonUK(addressLine1, addressLine2, addressLine3, addressLine4, country), _) =>
      Some((addressLine1, addressLine2, addressLine3, addressLine4, country.code))
    case _ => None
  }

  def apply(): Form[ResponsiblePersonAddress] = createForm(isUKAddress = false)
}
