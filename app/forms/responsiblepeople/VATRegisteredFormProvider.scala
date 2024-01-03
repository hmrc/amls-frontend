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
import models.responsiblepeople.{VATRegistered, VATRegisteredNo, VATRegisteredYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class VATRegisteredFormProvider @Inject()() extends Mappings {

  val length = 9

  private val booleanFieldName = "registeredForVAT"
  private val booleanError = "error.required.rp.registered.for.vat"

  def apply(): Form[VATRegistered] = Form[VATRegistered](
    mapping(
      booleanFieldName -> boolean(booleanError, booleanError),
      "vrnNumber" -> mandatoryIfTrue(
        booleanFieldName,
        text("error.rp.invalid.vat.number").verifying(
          firstError(
            correctLength(length, "error.invalid.vat.number.length"),
            regexp(vrnRegex, "error.invalid.vat.number")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(registeredForVAT: Boolean, vrnNumber: Option[String]): VATRegistered = (registeredForVAT, vrnNumber) match {
    case (true, Some(str)) => VATRegisteredYes(str)
    case (false, None) => VATRegisteredNo
    case _ => throw new IllegalArgumentException(s"Invalid combination of answers")
  }

  private def unapply(obj: VATRegistered): Option[(Boolean, Option[String])] = obj match {
    case VATRegisteredYes(vrnNumber) => Some((true, Some(vrnNumber)))
    case VATRegisteredNo => Some((false, None))
  }
}
