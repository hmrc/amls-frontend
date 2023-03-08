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

package forms

import forms.mappings.Mappings
import models.businessmatching.BusinessActivity
import play.api.data.Form
import play.api.data.Forms.seq

import javax.inject.Inject

class RemoveBusinessActivitiesFormProvider @Inject()() extends Mappings {

  def apply(maybeBusinessActivities: Seq[BusinessActivity]): Form[Seq[BusinessActivity]] = {

    Form[Seq[BusinessActivity]](
      "value" -> seq(enumerable[BusinessActivity]("error.required.bm.remove.service")).verifying(
          nonEmptySeq("error.required.bm.remove.service")
          /*TODO Add other constraints
            def maxLengthValidator(count: Int): Rule[Set[BusinessActivity], Set[BusinessActivity]] =
              Rule.fromMapping[Set[BusinessActivity], Set[BusinessActivity]] {
                case s if s.size == 2 && s.size == count => Invalid(Seq(ValidationError("error.required.bm.remove.leave.twobusinesses")))
                case s if s.size == count => Invalid(Seq(ValidationError("error.required.bm.remove.leave.one")))
                case s => Valid(s)
              }
           */
        )
      )
  }
}
