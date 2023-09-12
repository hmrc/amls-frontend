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

package forms.renewal

import forms.mappings.Mappings
import models.renewal.{InvolvedInOther, InvolvedInOtherNo, InvolvedInOtherYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class InvolvedInOtherFormProvider @Inject()() extends Mappings {

  private val booleanField = "involvedInOther"
  private val booleanError = "error.required.renewal.ba.involved.in.other"
  val length = 255
  def apply(): Form[InvolvedInOther] = Form[InvolvedInOther](
    mapping(
      booleanField -> boolean(booleanError, booleanError),
      "details" -> mandatoryIfTrue(booleanField, text("error.required.renewal.ba.involved.in.other.text")
        .verifying(
          firstError(
            maxLength(length, "error.invalid.maxlength.255.renewal.ba.involved.in.other"),
            regexp(basicPunctuationRegex, "error.text.validation.renewal.ba.involved.in.other")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(boolean: Boolean, details: Option[String]): InvolvedInOther = (boolean, details) match {
    case (false, _) => InvolvedInOtherNo
    case (true, Some(detail)) => InvolvedInOtherYes(detail)
  }

  private def unapply(obj: InvolvedInOther): Option[(Boolean, Option[String])] = obj match {
    case InvolvedInOtherYes(details) => Some((true, Some(details)))
    case InvolvedInOtherNo => Some((false, None))
  }
}
