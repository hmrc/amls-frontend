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

package forms.responsiblepeople

import forms.mappings.Mappings
import models.responsiblepeople.KnownBy
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class KnownByFormProvider @Inject() () extends Mappings {

  private val booleanFieldName = "hasOtherNames"
  private val booleanError     = "error.required.rp.hasOtherNames"

  val length                 = 140
  def apply(): Form[KnownBy] = Form[KnownBy](
    mapping(
      booleanFieldName -> boolean(booleanError, booleanError),
      "otherNames"     -> mandatoryIfTrue(
        booleanFieldName,
        text("error.required.rp.otherNames").verifying(
          firstError(
            maxLength(length, "error.invalid.rp.maxlength.140"),
            regexp(basicPunctuationRegex, "error.invalid.rp.char")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(hasOtherNames: Boolean, otherNames: Option[String]): KnownBy =
    KnownBy(Some(hasOtherNames), otherNames)

  private def unapply(obj: KnownBy): Option[(Boolean, Option[String])] = (obj.hasOtherNames, obj.otherNames) match {
    case (Some(bool), str) => Some((bool, str))
    case _                 => None
  }
}
