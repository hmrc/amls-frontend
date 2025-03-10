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
import models.businessdetails.CorrespondenceAddressUk
import play.api.data.Form

class CorrespondenceAddressUKFormProvider extends CorrespondenceAddressFormProvider[CorrespondenceAddressUk] {

  override val countryErrorKey: String = ""

  override def toObject
    : (String, String, String, Option[String], Option[String], Option[String], String) => CorrespondenceAddressUk = {
    case (yourName, businessName, line1, line2, line3, line4, postcode) =>
      CorrespondenceAddressUk(yourName, businessName, line1, line2, line3, line4, postcode)
  }

  override def fromObject: CorrespondenceAddressUk => Option[
    (String, String, String, Option[String], Option[String], Option[String], String)
  ] = {
    case CorrespondenceAddressUk(name, business, addressLine1, addressLine2, addressLine3, addressLine4, postcode) =>
      Some((name, business, addressLine1, addressLine2, addressLine3, addressLine4, postcode))
    case _                                                                                                         => None
  }

  def apply(): Form[CorrespondenceAddressUk] = createForm(true)
}
