/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, HighValueDealing}
import models.deregister.{DeRegisterSubscriptionRequest, DeregistrationReason}
import org.joda.time.LocalDate
import services.{AuthEnrolmentsService, StatusService}
import utils.{AckRefGenerator, AuthAction}
import views.html.deregister.deregistration_reason

import scala.concurrent.Future

class DeregistrationReasonController @Inject()(authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val dataCacheConnector: DataCacheConnector,
                                               amls: AmlsConnector,
                                               enrolments: AuthEnrolmentsService,
                                               statusService: StatusService) extends AmlsBaseController(ds) {

  def get = {
    authAction.async {
        implicit request =>
          dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map { businessMatching =>
            (for {
              bm <- businessMatching
              at <- bm.activities
            } yield {
              Ok(deregistration_reason(EmptyForm, at.businessActivities.contains(HighValueDealing)))
            }) getOrElse Ok(deregistration_reason(EmptyForm))
          }
    }
  }

  def post = authAction.async {
    implicit request =>
      Form2[DeregistrationReason](request.body) match {
        case f:InvalidForm => Future.successful(BadRequest(deregistration_reason(f)))
        case ValidForm(_, data) => {
          val deregistrationReasonOthers = data match {
            case DeregistrationReason.Other(reason) => reason.some
            case _ => None
          }
          val deregistration = DeRegisterSubscriptionRequest(
            AckRefGenerator(),
            LocalDate.now(),
            data,
            deregistrationReasonOthers
          )
          (for {
            regNumber <- OptionT(enrolments.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
            _ <- OptionT.liftF(amls.deregister(regNumber, deregistration, request.accountTypeId))
          } yield Redirect(controllers.routes.LandingController.get())) getOrElse InternalServerError("Unable to deregister the application")
        }
      }
  }
}
