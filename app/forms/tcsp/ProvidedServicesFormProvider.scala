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
import models.tcsp.ProvidedServices.Other
import models.tcsp.{ProvidedServices, TcspService}
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class ProvidedServicesFormProvider @Inject() () extends Mappings {

  private val checkboxError = "error.required.tcsp.provided_services.services"

  val length = 255

  def apply(): Form[ProvidedServices] = Form[ProvidedServices](
    mapping(
      "services" -> seq(enumerable[TcspService](checkboxError, checkboxError)(ProvidedServices.enumerable))
        .verifying(nonEmptySeq(checkboxError)),
      "details"  -> mandatoryIf(
        _.values.asJavaCollection.contains(Other("").toString),
        text("error.required.tcsp.provided_services.details").verifying(
          firstError(
            maxLength(length, "error.required.tcsp.provided_services.details.length"),
            regexp(basicPunctuationRegex, "error.required.tcsp.provided_services.details.punctuation")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(services: Seq[TcspService], maybeDetails: Option[String]): ProvidedServices =
    (services, maybeDetails) match {
      case (services, Some(detail)) if services.contains(Other("")) =>
        val modifiedTransactions = services.map(service => if (service == Other("")) Other(detail) else service)
        ProvidedServices(modifiedTransactions.toSet)
      case (services, Some(_)) if !services.contains(Other(""))     =>
        throw new IllegalArgumentException("Cannot have service details without Other TCSP service")
      case (services, None) if services.contains(Other(""))         =>
        throw new IllegalArgumentException("Cannot have Other TCSP service without service details")
      case (services, None) if !services.contains(Other(""))        => ProvidedServices(services.toSet)
    }

  private def unapply(obj: ProvidedServices): Option[(Seq[TcspService], Option[String])] = {
    val objTypes = obj.services.toSeq.map { x =>
      if (x.isInstanceOf[Other]) Other("") else x
    }

    val maybeName = obj.services.find(_.isInstanceOf[Other]).flatMap {
      case Other(details) => Some(details)
      case _              => None
    }

    Some((objTypes, maybeName))
  }
}
