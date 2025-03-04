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
import models.supervision.{AnotherBody, AnotherBodyNo, AnotherBodyYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class AnotherBodyFormProvider @Inject() () extends Mappings {

  val length = 140

  private val booleanFieldName   = "anotherBody"
  private val booleanErrorKey    = "error.required.supervision.anotherbody"
  def apply(): Form[AnotherBody] = Form[AnotherBody](
    mapping(
      booleanFieldName -> boolean(booleanErrorKey, booleanErrorKey),
      "supervisorName" -> mandatoryIfTrue(
        booleanFieldName,
        text("error.required.supervision.supervisor").verifying(
          firstError(
            maxLength(length, "error.invalid.supervision.supervisor.length.140"),
            regexp(basicPunctuationRegex, "error.invalid.supervision.supervisor")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(b: Boolean, optS: Option[String]): AnotherBody = (b, optS) match {
    case (false, None)      => AnotherBodyNo
    case (true, Some(name)) => AnotherBodyYes(name)
    case (true, None)       => throw new IllegalArgumentException("Cannot have Another Body without name")
    case (false, Some(_))   => throw new IllegalArgumentException("Cannot have name with no other body")
  }

  private def unapply(aB: AnotherBody): Option[(Boolean, Option[String])] = aB match {
    case AnotherBodyYes(name, _, _, _) => Some((true, Some(name)))
    case AnotherBodyNo                 => Some((false, None))
  }
}
