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

package controllers.deregister

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.deregister.DeregistrationReasonFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.businessmatching.BusinessMatching
import models.deregister.{DeRegisterSubscriptionRequest, DeregistrationReason}
import org.joda.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthEnrolmentsService
import utils.{AckRefGenerator, AuthAction}
import views.html.deregister.DeregistrationReasonView

import javax.inject.Inject

class DeregistrationReasonController @Inject()(authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val dataCacheConnector: DataCacheConnector,
                                               amls: AmlsConnector,
                                               enrolments: AuthEnrolmentsService,
                                               val cc: MessagesControllerComponents,
                                               formProvider: DeregistrationReasonFormProvider,
                                               view: DeregistrationReasonView) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map { businessMatching =>
        (for {
          bm <- businessMatching
          activities <- bm.activities
        } yield {
          Ok(view(formProvider(), activities.businessActivities.contains(HighValueDealing)))
        }) getOrElse Ok(view(formProvider()))
      }
  }

  def post: Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map { businessMatching =>
            (for {
              bm <- businessMatching
              activities <- bm.activities
            } yield {
              BadRequest(view(formWithErrors, activities.businessActivities.contains(HighValueDealing)))
            }) getOrElse BadRequest(view(formWithErrors))
          },
        data => {
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
          } yield Redirect(controllers.routes.LandingController.get)) getOrElse InternalServerError("Unable to deregister the application")
        }
    )
  }
}
