/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.asp

import forms.generic.BooleanFormProvider
import models.asp.{OtherBusinessTaxMatters, OtherBusinessTaxMattersNo, OtherBusinessTaxMattersYes}
import play.api.data.Form

import javax.inject.Inject

class OtherBusinessTaxMattersFormProvider @Inject()() extends BooleanFormProvider {

  def apply(): Form[OtherBusinessTaxMatters] = createForm[OtherBusinessTaxMatters](
    "otherBusinessTaxMatters", "error.required.asp.other.business.tax.matters"
    )(apply, unapply)

  private def apply(boolean: Boolean): OtherBusinessTaxMatters =
    if(boolean) OtherBusinessTaxMattersYes else OtherBusinessTaxMattersNo
  private def unapply(obj: OtherBusinessTaxMatters) = obj match {
    case OtherBusinessTaxMattersYes => true
    case OtherBusinessTaxMattersNo => false
  }
}