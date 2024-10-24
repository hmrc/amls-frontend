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

package controllers.deregister


import cats.data.EitherT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.deregister.{DeRegisterSubscriptionRequest, DeregistrationReason}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.AuthEnrolmentsService
import utils.{AckRefGenerator, AuthAction, AuthorisedRequest}
import views.html.deregister.DeregistrationCheckYourAnswersView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.Future

class DeregistrationCheckYourAnswersController @Inject()(authAction: AuthAction,
                                                         ds: CommonPlayDependencies,
                                                         dataCacheConnector: DataCacheConnector,
                                                         amlsConnector: AmlsConnector,
                                                         authEnrolmentsService: AuthEnrolmentsService,
                                                         cc: MessagesControllerComponents,
                                                         view: DeregistrationCheckYourAnswersView) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
      fetchDeregistrationReason()
        .map(deregistrationReason => Ok(view(deregistrationReason = deregistrationReason)))
        .value.map(_.fold(identity, identity))
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>

    val eitherT: EitherT[Future, Result, Result] = for {
      deregistrationReason <- fetchDeregistrationReason()
      deRegisterSubscriptionRequest = DeRegisterSubscriptionRequest(
        acknowledgementReference = AckRefGenerator(),
        deregistrationDate = LocalDate.now(),
        deregistrationReason = deregistrationReason,
        deregReasonOther = deregistrationReasonOthers(deregistrationReason)
      )
      amlsRegistrationNumber <- EitherT.fromOptionF(
        authEnrolmentsService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier),
        redirectToLandingPage
      )
      _ <- EitherT.right(amlsConnector.deregister(
        amlsRegistrationNumber = amlsRegistrationNumber,
        request = deRegisterSubscriptionRequest,
        accountTypeId = request.accountTypeId
      ))
    } yield Redirect(controllers.deregister.routes.DeregistrationConfirmationController.get)
    eitherT.value.map(_.fold(identity, identity))
  }

  private val redirectToLandingPage = Redirect(controllers.routes.LandingController.start(true))

  private def fetchDeregistrationReason()(implicit request: AuthorisedRequest[AnyContent]) = EitherT.fromOptionF[Future, Result, DeregistrationReason](
    dataCacheConnector.fetch[DeregistrationReason](request.credId, DeregistrationReason.key),
    redirectToLandingPage
  )

  private def deregistrationReasonOthers(deregistrationReason: DeregistrationReason): Option[String] = deregistrationReason match {
    case DeregistrationReason.Other(reason) => Some(reason)
    case _ => None
  }

}
