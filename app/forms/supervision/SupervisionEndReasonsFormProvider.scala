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

import forms.mappings.Mappings
import models.supervision.SupervisionEndReasons
import play.api.data.Form

import javax.inject.Inject

class SupervisionEndReasonsFormProvider @Inject() () extends Mappings {

  val length                               = 255
  def apply(): Form[SupervisionEndReasons] = Form[SupervisionEndReasons](
    "endingReason" -> text("error.required.supervision.reason")
      .verifying(
        firstError(
          maxLength(length, "error.supervision.end.reason.invalid.maxlength.255"),
          regexp(basicPunctuationRegex, "error.supervision.end.reason.invalid")
        )
      )
      .transform[SupervisionEndReasons](SupervisionEndReasons.apply, _.endingReason)
  )
}
