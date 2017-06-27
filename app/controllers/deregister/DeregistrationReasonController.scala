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

package controllers.deregister

import javax.inject.Inject

import cats.implicits._
import cats.data.OptionT
import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, HighValueDealing}
import models.deregister.{DeRegisterSubscriptionRequest, DeregistrationReason}
import org.joda.time.LocalDate
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.FeatureToggle
import views.html.deregister.deregistration_reason

import scala.concurrent.Future

class DeregistrationReasonController @Inject()(val authConnector: AuthConnector,
                                               val dataCacheConnector: DataCacheConnector,
                                               amls: AmlsConnector,
                                               enrolments: AuthEnrolmentsService,
                                               statusService: StatusService) extends BaseController {

  def get = FeatureToggle(ApplicationConfig.allowDeRegisterToggle) {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map { businessMatching =>
            (for {
              bm <- businessMatching
              at <- bm.activities
            } yield {
              Ok(deregistration_reason(EmptyForm, at.businessActivities.contains(HighValueDealing)))
            }) getOrElse Ok(deregistration_reason(EmptyForm))
          }
    }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      Form2[DeregistrationReason](request.body) match {
        case f:InvalidForm => Future.successful(BadRequest(deregistration_reason(f)))
        case ValidForm(_, data) => {
          val deregistrationReasonOthers = data match {
            case DeregistrationReason.Other(reason) => reason.some
            case _ => None
          }
          val deregistration = DeRegisterSubscriptionRequest(
            DeRegisterSubscriptionRequest.DefaultAckReference,
            LocalDate.now(),
            data,
            deregistrationReasonOthers
          )
          (for {
            regNumber <- OptionT(enrolments.amlsRegistrationNumber)
            _ <- OptionT.liftF(amls.deregister(regNumber, deregistration))
          } yield Redirect(controllers.routes.LandingController.get())) getOrElse InternalServerError("Unable to withdraw the application")
        }
      }
  }

}
