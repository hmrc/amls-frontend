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

package controllers

import cats.data.OptionT
import cats.implicits._
import config.AppConfig
import connectors.{AmlsConnector, DataCacheConnector, _}
import javax.inject.{Inject, Singleton}
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredYes}
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthAction, BusinessName, ControllerHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BacsConfirmationController @Inject()(authAction: AuthAction,
                                           private[controllers] implicit val dataCacheConnector: DataCacheConnector,
                                           private[controllers] implicit val amlsConnector: AmlsConnector,
                                           private[controllers] implicit val statusService: StatusService,
                                           private[controllers] val authenticator: AuthenticatorConnector,
                                           private[controllers] val enrolmentService: AuthEnrolmentsService) extends DefaultBaseController {

  def bacsConfirmation() = authAction.async {
      implicit request =>
        val okResult = for {
          _ <- OptionT.liftF(authenticator.refreshProfile)
          refNo <- OptionT(enrolmentService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
          status <- OptionT.liftF(statusService.getReadStatus(refNo, request.accountTypeId))
          name <- BusinessName.getName(request.credId, status.safeId, request.accountTypeId)
          businessDetails <- OptionT(dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key))
        } yield businessDetails.previouslyRegistered match {
          case Some(PreviouslyRegisteredYes(_)) => Ok(views.html.confirmation.confirmation_bacs_transitional_renewal(name))
          case _ => Ok(views.html.confirmation.confirmation_bacs(name))
        }

        okResult getOrElse InternalServerError("Unable to get BACS confirmation")
  }
}