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

package forms.businessdetails

import forms.mappings.Mappings
import models.businessdetails.VATRegistered
import models.businessdetails.{VATRegisteredNo, VATRegisteredYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class VATRegisteredFormProvider @Inject()() extends Mappings {

  val length = 9
  def apply(): Form[VATRegistered] = Form[VATRegistered](
    mapping(
      "registeredForVAT" -> boolean("error.required.atb.registered.for.vat", "error.required.atb.registered.for.vat"),
      "vrnNumber" -> mandatoryIfTrue("registeredForVAT", text("error.required.vat.number").transform[String](_.replace(" ","").trim, x => x)
        .verifying(
          firstError(
            correctLength(length, "error.invalid.vat.number.length"),
            regexp(vrnRegex, "error.invalid.vat.number")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(b: Boolean, s: Option[String]): VATRegistered = (b, s) match {
    case (false, _) => VATRegisteredNo
    case (true, Some(str)) => VATRegisteredYes(str)
    case _ => throw new IllegalArgumentException("No VAT Number available to bind from form")
  }

  private def unapply(obj: VATRegistered): Option[(Boolean, Option[String])] = obj match {
    case VATRegisteredNo => Some((false, None))
    case VATRegisteredYes(vatNumber) => Some((true, Some(vatNumber)))
    case _ => None
  }
}
