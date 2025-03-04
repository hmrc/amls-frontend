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

import forms.generic.BooleanFormProvider
import models.tcsp.{ComplexCorpStructureCreation, ComplexCorpStructureCreationNo, ComplexCorpStructureCreationYes}
import play.api.data.Form

import javax.inject.Inject

class ComplexCorpStructureCreationFormProvider @Inject() () extends BooleanFormProvider {

  def apply(): Form[ComplexCorpStructureCreation] = createForm[ComplexCorpStructureCreation](
    "complexCorpStructureCreation",
    "error.required.tcsp.complex.corporate.structures"
  )(apply, unapply)

  private def apply(boolean: Boolean): ComplexCorpStructureCreation =
    if (boolean) ComplexCorpStructureCreationYes else ComplexCorpStructureCreationNo
  private def unapply(obj: ComplexCorpStructureCreation)            = obj match {
    case ComplexCorpStructureCreationYes => true
    case ComplexCorpStructureCreationNo  => false
  }
}
