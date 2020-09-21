/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.{AmlsConnector, DataCacheConnector, _}
import javax.inject.{Inject, Singleton}
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredYes}
import play.api.mvc.MessagesControllerComponents
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthAction, BusinessName}
import views.html.confirmation.{confirmation_bacs, confirmation_bacs_transitional_renewal}

@Singleton
class BacsConfirmationController @Inject()(authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           private[controllers] implicit val dataCacheConnector: DataCacheConnector,
                                           private[controllers] implicit val amlsConnector: AmlsConnector,
                                           private[controllers] implicit val statusService: StatusService,
                                           private[controllers] val authenticator: AuthenticatorConnector,
                                           private[controllers] val enrolmentService: AuthEnrolmentsService,
                                           val cc: MessagesControllerComponents,
                                           confirmation_bacs_transitional_renewal: confirmation_bacs_transitional_renewal,
                                           confirmation_bacs: confirmation_bacs) extends AmlsBaseController(ds, cc) {

  def bacsConfirmation() = authAction.async {
      implicit request =>
        val okResult = for {
          _ <- OptionT.liftF(authenticator.refreshProfile)
          refNo <- OptionT(enrolmentService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
          status <- OptionT.liftF(statusService.getReadStatus(refNo, request.accountTypeId))
          name <- BusinessName.getName(request.credId, status.safeId, request.accountTypeId)
          businessDetails <- OptionT(dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key))
        } yield businessDetails.previouslyRegistered match {
          case Some(PreviouslyRegisteredYes(_)) => Ok(confirmation_bacs_transitional_renewal(name))
          case _ => Ok(confirmation_bacs(name))
        }

        okResult getOrElse InternalServerError("Unable to get BACS confirmation")
  }
}