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

package forms.businessmatching

import forms.mappings.Mappings
import models.businessmatching.BusinessActivity
import play.api.data.Form
import play.api.data.Forms.seq
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class RemoveBusinessActivitiesFormProvider @Inject() () extends Mappings {

  def apply(noOfActivities: Int): Form[Seq[BusinessActivity]] = {

    def notAllActivitiesRemoved: Constraint[Seq[_]] =
      Constraint {
        case seq if seq.size == 2 && seq.size == noOfActivities =>
          Invalid("error.required.bm.remove.leave.twobusinesses")
        case seq if seq.size == noOfActivities                  =>
          Invalid("error.required.bm.remove.leave.one")
        case _                                                  =>
          Valid
      }

    val error = if (noOfActivities > 2) {
      "error.required.bm.remove.service.multiple"
    } else {
      "error.required.bm.remove.service"
    }

    Form[Seq[BusinessActivity]](
      "value" -> seq(enumerable[BusinessActivity](error)).verifying(nonEmptySeq(error), notAllActivitiesRemoved)
    )
  }
}
