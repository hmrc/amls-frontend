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
import models.supervision.{ProfessionalBodyMember, ProfessionalBodyMemberNo, ProfessionalBodyMemberYes}
import play.api.data.Form

import javax.inject.Inject

class MemberOfProfessionalBodyFormProvider @Inject() () extends BooleanFormProvider {

  def apply(): Form[ProfessionalBodyMember] = createForm[ProfessionalBodyMember](
    "isAMember",
    "error.required.supervision.business.a.member"
  )(apply, unapply)

  private def apply(boolean: Boolean): ProfessionalBodyMember =
    if (boolean) ProfessionalBodyMemberYes else ProfessionalBodyMemberNo

  private def unapply(obj: ProfessionalBodyMember): Boolean = obj match {
    case ProfessionalBodyMemberYes => true
    case ProfessionalBodyMemberNo  => false
  }
}
