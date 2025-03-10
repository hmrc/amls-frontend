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
import models.responsiblepeople.PreviousName
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class LegalNameInputFormProvider @Inject() () extends Mappings {

  val length = 35

  def apply(): Form[PreviousName] = Form[PreviousName](
    mapping(
      "firstName"  -> text("error.rp.previous.first.invalid").verifying(
        firstError(
          maxLength(length, "error.rp.previous.first.length.invalid"),
          regexp(nameRegex, "error.rp.previous.first.char.invalid")
        )
      ),
      "middleName" -> optional(
        text("").verifying(
          firstError(
            maxLength(length, "error.rp.previous.middle.length.invalid"),
            regexp(nameRegex, "error.rp.previous.middle.char.invalid")
          )
        )
      ),
      "lastName"   -> text("error.rp.previous.last.invalid").verifying(
        firstError(
          maxLength(length, "error.rp.previous.last.length.invalid"),
          regexp(nameRegex, "error.rp.previous.last.char.invalid")
        )
      )
    )(apply)(unapply)
  )

  private def apply(first: String, middle: Option[String], last: String): PreviousName =
    PreviousName(Some(true), Some(first), middle, Some(last))

  private def unapply(obj: PreviousName): Option[(String, Option[String], String)] =
    (obj.firstName, obj.lastName) match {
      case (Some(first), Some(last)) => Some((first, obj.middleName, last))
      case _                         => None
    }
}
