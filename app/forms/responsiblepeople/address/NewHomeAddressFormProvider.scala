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

import models.responsiblepeople.{NewHomeAddress, PersonAddress}

import javax.inject.Inject

class NewHomeAddressFormProvider @Inject() () extends IsAddressUKFormProvider[NewHomeAddress] {

  override val error = "error.required.uk.or.overseas.address.new.home"

  override def apply(personAddress: PersonAddress): NewHomeAddress = NewHomeAddress(personAddress)

  override def unapply(a: NewHomeAddress): Option[PersonAddress] = Some(a.personAddress)
}
