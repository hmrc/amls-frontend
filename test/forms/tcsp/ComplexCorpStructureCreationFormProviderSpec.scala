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

package forms.tcsp

import forms.behaviours.BooleanFieldBehaviours
import models.tcsp.{ComplexCorpStructureCreation, ComplexCorpStructureCreationNo, ComplexCorpStructureCreationYes}
import play.api.data.Form

class ComplexCorpStructureCreationFormProviderSpec extends BooleanFieldBehaviours[ComplexCorpStructureCreation] {

  override val form: Form[ComplexCorpStructureCreation] = new ComplexCorpStructureCreationFormProvider()()
  override val fieldName: String                        = "complexCorpStructureCreation"
  override val errorMessage: String                     = "error.required.tcsp.complex.corporate.structures"

  "ComplexCorpStructureCreationFormProvider" must {

    behave like booleanFieldWithModel(
      ComplexCorpStructureCreationYes,
      ComplexCorpStructureCreationNo
    )
  }
}
