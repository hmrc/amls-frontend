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

package forms.supervision

import forms.generic.BooleanFormProvider
import models.supervision.{ProfessionalBody, ProfessionalBodyNo, ProfessionalBodyYes}
import play.api.data.Form

import javax.inject.Inject

class PenalisedByProfessionalFormProvider @Inject() () extends BooleanFormProvider {

  def apply(): Form[ProfessionalBody] = createForm[ProfessionalBody](
    "penalised",
    "error.required.professionalbody.penalised.by.professional.body"
  )(apply, unapply)

  private def apply(boolean: Boolean): ProfessionalBody =
    if (boolean) ProfessionalBodyYes("") else ProfessionalBodyNo

  private def unapply(obj: ProfessionalBody): Boolean = obj match {
    case ProfessionalBodyYes(_) => true
    case ProfessionalBodyNo     => false
  }

}
