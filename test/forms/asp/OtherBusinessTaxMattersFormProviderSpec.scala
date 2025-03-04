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

package forms.asp

import forms.behaviours.BooleanFieldBehaviours
import models.asp.{OtherBusinessTaxMatters, OtherBusinessTaxMattersNo, OtherBusinessTaxMattersYes}
import play.api.data.Form
class OtherBusinessTaxMattersFormProviderSpec extends BooleanFieldBehaviours[OtherBusinessTaxMatters] {

  override val form: Form[OtherBusinessTaxMatters] = new OtherBusinessTaxMattersFormProvider()()
  override val fieldName: String                   = "otherBusinessTaxMatters"
  override val errorMessage: String                = "error.required.asp.other.business.tax.matters"

  "OtherBusinessTaxMattersFormProvider" must {

    behave like booleanFieldWithModel(
      OtherBusinessTaxMattersYes,
      OtherBusinessTaxMattersNo
    )
  }
}
