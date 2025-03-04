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

import forms.mappings.Mappings
import models.Country
import models.responsiblepeople.{PersonAddress, PersonAddressNonUK, PersonAddressUK}
import play.api.data.Form
import play.api.data.Forms.mapping

trait IsAddressUKFormProvider[A] extends Mappings {

  protected val error: String

  /*
    TODO: This needs to be changed, current model creation is not fit for purpose
   */
  def apply(): Form[A] = Form[A](
    mapping(
      "isUK" -> boolean(error, error)
        .transform[PersonAddress](
          if (_) PersonAddressUK("", None, None, None, "")
          else PersonAddressNonUK("", None, None, None, Country("", "")),
          {
            case PersonAddressUK(_, _, _, _, _)    => true
            case PersonAddressNonUK(_, _, _, _, _) => false
          }
        )
    )(apply)(unapply)
  )

  def apply(personAddress: PersonAddress): A
  def unapply(a: A): Option[PersonAddress]
}
