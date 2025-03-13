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
import models.supervision.ProfessionalBodies.Other
import models.supervision.{BusinessType, ProfessionalBodies}
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class WhichProfessionalBodyFormProvider @Inject() () extends Mappings {

  private val checkboxError = "error.required.supervision.one.professional.body"

  val length = 255 // TODO This looks way too long

  def apply(): Form[ProfessionalBodies] = Form[ProfessionalBodies](
    mapping(
      "businessType"         -> seq(enumerable[BusinessType](checkboxError, checkboxError)(ProfessionalBodies.enumerable))
        .verifying(nonEmptySeq(checkboxError)),
      "specifyOtherBusiness" -> mandatoryIf(
        _.values.asJavaCollection.contains(Other("").toString),
        text("error.required.supervision.business.details").verifying(
          firstError(
            maxLength(length, "error.invalid.supervision.business.details.length.255"),
            regexp(basicPunctuationRegex, "error.invalid.supervision.business.details")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(businessType: Seq[BusinessType], maybeDetails: Option[String]): ProfessionalBodies =
    (businessType, maybeDetails) match {
      case (services, Some(detail)) if services.contains(Other("")) =>
        val modifiedTransactions = services.map(service => if (service == Other("")) Other(detail) else service)
        ProfessionalBodies(modifiedTransactions.toSet)
      case (services, Some(_)) if !services.contains(Other(""))     =>
        throw new IllegalArgumentException("Cannot have service details without Other TCSP service")
      case (services, None) if services.contains(Other(""))         =>
        throw new IllegalArgumentException("Cannot have Other TCSP service without service details")
      case (services, None) if !services.contains(Other(""))        => ProfessionalBodies(services.toSet)
    }

  private def unapply(obj: ProfessionalBodies): Option[(Seq[BusinessType], Option[String])] = {
    val objTypes = obj.businessTypes.toSeq.map { x =>
      if (x.isInstanceOf[Other]) Other("") else x
    }

    val maybeName = obj.businessTypes.find(_.isInstanceOf[Other]).flatMap {
      case Other(details) => Some(details)
      case _              => None
    }

    Some((objTypes, maybeName))
  }
}
