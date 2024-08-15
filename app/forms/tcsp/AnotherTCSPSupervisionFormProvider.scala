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

package forms.tcsp

import forms.mappings.Mappings
import models.tcsp.{ServicesOfAnotherTCSP, ServicesOfAnotherTCSPNo, ServicesOfAnotherTCSPYes}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class AnotherTCSPSupervisionFormProvider @Inject()() extends Mappings {

  val minLength = 8
  val maxLength = 15

  private val radioFieldName = "servicesOfAnotherTCSP"
  private val radioError = "error.required.tcsp.services.another.tcsp.registered"
  private val lengthError = "error.tcsp.services.another.tcsp.number.length"

  def apply(): Form[ServicesOfAnotherTCSP] = Form[ServicesOfAnotherTCSP](
    mapping(
      radioFieldName -> boolean(radioError, radioError),
      "mlrRefNumber" -> optional(textAllowWhitespace("error.required.tcsp.services.another.tcsp.number")
        .verifying(
          firstError(
            minLength(minLength, lengthError),
            maxLength(maxLength, lengthError),
            regexp(basicPunctuationRegex, "error.tcsp.services.another.tcsp.number.punctuation")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(b: Boolean, s: Option[String]): ServicesOfAnotherTCSP = (b, s) match {
    case (false, _) => ServicesOfAnotherTCSPNo
    case (true, refNo) => ServicesOfAnotherTCSPYes(refNo)
  }

  private def unapply(obj: ServicesOfAnotherTCSP): Option[(Boolean, Option[String])] = obj match {
    case ServicesOfAnotherTCSPNo => Some((false, None))
    case ServicesOfAnotherTCSPYes(refNo) => Some((true, refNo))
    case _ => None
  }
}