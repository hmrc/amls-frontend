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

package forms.declaration

import forms.mappings.Mappings
import models.declaration.WhoIsRegistering
import play.api.data.Form

import javax.inject.Inject

class WhoIsRegisteringFormProvider @Inject() () extends Mappings {

  def apply(errorSuffix: String): Form[WhoIsRegistering] = {

    val commonError = "error.required.declaration.who.is.declaring.this"

    Form(
      "person" -> text(s"$commonError.$errorSuffix")
        .transform[WhoIsRegistering](WhoIsRegistering.apply, _.person)
    )
  }
}
