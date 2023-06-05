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

package forms.msb

import forms.mappings.Mappings
import models.moneyservicebusiness.{BusinessUseAnIPSP, BusinessUseAnIPSPNo, BusinessUseAnIPSPYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class BusinessUseAnIPSPFormProvider @Inject()() extends Mappings {

  private val booleanFieldName = "useAnIPSP"
  private val booleanError = "error.required.msb.ipsp"

  val length = 140

  private val referenceNumberRegex = """^[0-9]{8}|[a-zA-Z0-9]{15}$"""

  def apply(): Form[BusinessUseAnIPSP] = Form[BusinessUseAnIPSP](
    mapping(
      booleanFieldName -> boolean(booleanError, booleanError),
      "name" -> mandatoryIfTrue(
        booleanFieldName,
        text("error.required.msb.ipsp.name").verifying(
          firstError(
            maxLength(length, "error.invalid.msb.ipsp.name"),
            regexp(basicPunctuationRegex, "error.invalid.msb.ipsp.format"),
          )
        )
      ),
      "referenceNumber" -> mandatoryIfTrue(
        booleanFieldName,
        text("error.invalid.mlr.number").verifying(
          regexp(referenceNumberRegex, "error.invalid.mlr.number")
        )
      )
    )(apply)(unapply)
  )

  private def apply(useAnIPSP: Boolean, nameOpt: Option[String], referenceNumber: Option[String]): BusinessUseAnIPSP = {
    (useAnIPSP, nameOpt, referenceNumber) match {
      case (true, Some(name), Some(refNo)) => BusinessUseAnIPSPYes(name, refNo)
      case (false, _, _) => BusinessUseAnIPSPNo
      case _ => throw new IllegalArgumentException("Invalid form entry, cannot make instance of BusinessUseAnIPSP")
    }
  }

  private def unapply(obj: BusinessUseAnIPSP): Option[(Boolean, Option[String], Option[String])] = {
    obj match {
      case BusinessUseAnIPSPYes(name, reference) => Some((true, Some(name), Some(reference)))
      case BusinessUseAnIPSPNo => Some((false, None, None))
    }
  }
}
