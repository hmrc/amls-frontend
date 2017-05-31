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
import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import models.deregister.{DeRegisterReason, DeRegisterSubscriptionRequest}
import org.joda.time.LocalDate
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.deregister_application

import scala.concurrent.Future

class DeRegisterApplicationController @Inject()
(
  val authConnector: AuthConnector,
  cache: DataCacheConnector,
  statusService: StatusService,
  enrolments: AuthEnrolmentsService,
  amls: AmlsConnector
) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request =>

      if(!ApplicationConfig.allowDeRegisterToggle) {
        Future.successful(NotFound)
      } else {
        val maybeProcessingDate = for {
          status <- OptionT.liftF(statusService.getDetailedStatus)
          response <- OptionT.fromOption[Future](status._2)
        } yield response.processingDate

        (for {
          bm <- OptionT(cache.fetch[BusinessMatching](BusinessMatching.key))
          details <- OptionT.fromOption[Future](bm.reviewDetails)
          processingDate <- maybeProcessingDate
          amlsRegNumber <- OptionT(enrolments.amlsRegistrationNumber)
        } yield {
          Ok(deregister_application(details.businessName, processingDate, amlsRegNumber))
        }) getOrElse InternalServerError("Could not show the de-register page")
      }
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      val maybeRequest = for {
        regNumber <- OptionT(enrolments.amlsRegistrationNumber)
        _ <- OptionT.liftF(amls.deregister(regNumber, DeRegisterSubscriptionRequest("A" * 32, LocalDate.now, DeRegisterReason.OutOfScope)))
      } yield Redirect(routes.StatusController.get())

      maybeRequest getOrElse InternalServerError("Could not de-register the subscription")
  }
}
