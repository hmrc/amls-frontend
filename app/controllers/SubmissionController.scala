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

import connectors.AuthenticatorConnector
import exceptions.{DuplicateEnrolmentException, DuplicateSubscriptionException, InvalidEnrolmentCredentialsException}
import javax.inject.{Inject, Singleton}
import models.status._
import models.{SubmissionResponse, SubscriptionResponse}
import play.api.Logger
import play.api.mvc.MessagesControllerComponents
import services.{RenewalService, StatusService, SubmissionService}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.AuthAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubmissionController @Inject()(val subscriptionService: SubmissionService,
                                     val statusService: StatusService,
                                     val renewalService: RenewalService,
                                     val authenticator: AuthenticatorConnector,
                                     authAction: AuthAction,
                                     val ds: CommonPlayDependencies,
                                     val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  private def handleRenewalAmendment(credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
                                    (implicit headerCarrier: HeaderCarrier) = {

    renewalService.getRenewal(credId) flatMap {
      case Some(renewal) => subscriptionService.renewalAmendment(credId, amlsRegistrationNumber, accountTypeId, renewal)
      case _ => subscriptionService.variation(credId, amlsRegistrationNumber, accountTypeId)
    }
  }

  def post() = authAction.async {
      implicit request => {
        statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId).flatMap[SubmissionResponse](status =>
          subscribeBasedOnStatus(status, request.groupIdentifier, request.credId, request.amlsRefNumber, request.accountTypeId))
      }.flatMap {
        case SubscriptionResponse(_, _, _, Some(true)) =>
          authenticator.refreshProfile map { _ =>
            Redirect(controllers.routes.LandingController.get())
          }
        case _ => Future.successful(Redirect(controllers.routes.ConfirmationController.get()))
      } recoverWith {
        case _: DuplicateEnrolmentException =>
          Logger.info("[SubmissionController][post] handling DuplicateEnrolmentException")
          Future.successful(Ok(views.html.submission.duplicate_enrolment()))
        case e: DuplicateSubscriptionException =>
          Logger.info("[SubmissionController][post] handling DuplicateSubscriptionException")
          Future.successful(Ok(views.html.submission.duplicate_submission(e.message)))
        case _: InvalidEnrolmentCredentialsException =>
          Logger.info("[SubmissionController][post] handling InvalidEnrolmentCredentialsException")
          Future.successful(Ok(views.html.submission.wrong_credential_type()))
        case _: BadRequestException =>
          Logger.info("[SubmissionController][post] handling BadRequestException")
          Future.successful(Ok(views.html.submission.bad_request()))
      }
  }

  private def subscribeBasedOnStatus(status: SubmissionStatus, groupIdentifier: Option[String], credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
                                    (implicit hc: HeaderCarrier) =
    status match {
      case SubmissionReadyForReview => subscriptionService.update(credId, amlsRegistrationNumber, accountTypeId)
      case SubmissionDecisionApproved => subscriptionService.variation(credId, amlsRegistrationNumber, accountTypeId)
      case ReadyForRenewal(_) => renewalService.getRenewal(credId) flatMap {
        case Some(renewal) => subscriptionService.renewal(credId, amlsRegistrationNumber, accountTypeId, renewal)
        case _ => subscriptionService.variation(credId, amlsRegistrationNumber, accountTypeId)
      }
      case RenewalSubmitted(_) => handleRenewalAmendment(credId, amlsRegistrationNumber, accountTypeId)
      case _ => subscriptionService.subscribe(credId, accountTypeId, groupIdentifier)
  }
}