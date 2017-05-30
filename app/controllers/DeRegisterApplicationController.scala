/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import play.api.i18n.Messages
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.deregister_application

import scala.concurrent.Future

class DeRegisterApplicationController @Inject()
(
  val authConnector: AuthConnector,
  messages: Messages,
  cache: DataCacheConnector,
  statusService: StatusService,
  enrolments: AuthEnrolmentsService
) extends BaseController {
  def get() = Authorised.async {
    implicit authContext => implicit request =>

      val maybeProcessingDate = for {
        status <- OptionT.liftF(statusService.getDetailedStatus)
        response <- OptionT.fromOption[Future](status._2)
      } yield response.processingDate

      (for {
        bm <- OptionT(cache.fetch[BusinessMatching](BusinessMatching.key))
        details <- OptionT.fromOption[Future](bm.reviewDetails)
        processingDate <- maybeProcessingDate
        amlsRegNumber <- OptionT(enrolments.amlsRegistrationNumber)
      } yield Ok(deregister_application(details.businessName, processingDate, amlsRegNumber))) getOrElse InternalServerError("Could not show the de-register page")
  }
}
