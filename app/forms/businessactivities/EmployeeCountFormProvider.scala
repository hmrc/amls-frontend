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

package forms.businessactivities

import forms.mappings.Mappings
import models.businessactivities.EmployeeCount
import play.api.data.Form

import javax.inject.Inject

class EmployeeCountFormProvider @Inject() () extends Mappings {

  val length                       = 11
  val regex                        = "^[0-9]+$"
  def apply(): Form[EmployeeCount] = Form[EmployeeCount](
    "employeeCount" -> text("error.empty.ba.employee.count")
      .verifying(
        firstError(
          maxLength(length, "error.max.length.ba.employee.count"),
          regexp(regex, "error.invalid.ba.employee.count")
        )
      )
      .transform[EmployeeCount](EmployeeCount.apply, _.employeeCount)
  )

}
