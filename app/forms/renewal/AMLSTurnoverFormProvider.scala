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

package forms.renewal

import forms.mappings.Mappings
import models.renewal.AMLSTurnover
import play.api.data.Form
import play.api.i18n.Messages

import javax.inject.Inject

class AMLSTurnoverFormProvider @Inject() () extends Mappings {

  private val singleServiceError                                                             = "error.required.renewal.ba.turnover.from.mlr.single.service"
  private val multiServiceError                                                              = "error.required.renewal.ba.turnover.from.mlr"
  def apply(service: Option[String] = None)(implicit messages: Messages): Form[AMLSTurnover] = Form(
    "turnover" -> enumerable[AMLSTurnover](
      service.fold(multiServiceError)(key => messages(singleServiceError, key))
    )
  )
}
