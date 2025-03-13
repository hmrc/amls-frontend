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

package forms.deregister

import forms.mappings.Mappings
import models.deregister.DeregistrationReason
import models.deregister.DeregistrationReason.Other
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class DeregistrationReasonFormProvider @Inject() () extends Mappings {

  private def error = "error.required.deregistration.reason"
  val length        = 40

  def apply(): Form[DeregistrationReason]                                                            = Form[DeregistrationReason](
    mapping(
      "deregistrationReason" -> enumerable[DeregistrationReason](error, error),
      "specifyOtherReason"   -> mandatoryIf(
        _.values.asJavaCollection.contains(Other("").toString),
        text("error.required.deregistration.reason.input").verifying(
          firstError(
            maxLength(length, "error.required.deregistration.reason.length"),
            regexp(basicPunctuationRegex, "error.required.deregistration.reason.format")
          )
        )
      )
    )(apply)(unapply)
  )
  private def apply(reason: DeregistrationReason, otherReason: Option[String]): DeregistrationReason =
    (reason, otherReason) match {
      case (_: DeregistrationReason.Other, Some(reason)) => Other(reason)
      case (_: DeregistrationReason.Other, None)         =>
        throw new IllegalArgumentException("Description is required when Other is selected")
      case (reason, _)                                   => reason
    }

  private def unapply(obj: DeregistrationReason): Option[(DeregistrationReason, Option[String])] = obj match {
    case Other(otherReason) => Some((Other(""), Some(otherReason)))
    case reason             => Some((reason, None))
  }
}
