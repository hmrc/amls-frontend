/*
 * Copyright 2021 HM Revenue & Customs
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
import services.{RenewalService, SectionsProvider, StatusService, SubmissionService}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.{AuthAction, DeclarationHelper}
import views.html.submission.{bad_request, duplicate_enrolment, duplicate_submission, wrong_credential_type}
import play.api.mvc.Request

import scala.concurrent.Future

@Singleton
class SubmissionController @Inject()(val subscriptionService: SubmissionService,
                                     val statusService: StatusService,
                                     val renewalService: RenewalService,
                                     val authenticator: AuthenticatorConnector,
                                     authAction: AuthAction,
                                     val ds: CommonPlayDependencies,
                                     val cc: MessagesControllerComponents,
                                     val sectionsProvider: SectionsProvider,
                                     duplicate_enrolment: duplicate_enrolment,
                                     duplicate_submission: duplicate_submission,
                                     wrong_credential_type: wrong_credential_type,
                                     bad_request: bad_request) extends AmlsBaseController(ds, cc) {

  private def handleRenewalAmendment(credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
                                    (implicit headerCarrier: HeaderCarrier) = {

    renewalService.getRenewal(credId) flatMap {
      case Some(renewal) => subscriptionService.renewalAmendment(credId, amlsRegistrationNumber, accountTypeId, renewal)
      case _ => subscriptionService.variation(credId, amlsRegistrationNumber, accountTypeId)
    }
  }

  def post() = authAction.async {
    implicit request =>
      DeclarationHelper.sectionsComplete(request.credId, sectionsProvider) flatMap {
        case true => {
          // $COVERAGE-OFF$
          Logger.info("[SubmissionController][post]:true")
          // $COVERAGE-ON$
          statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId).flatMap[SubmissionResponse](status =>
            subscribeBasedOnStatus(status, request.groupIdentifier, request.credId, request.amlsRefNumber, request.accountTypeId))
          }.flatMap {
          case SubscriptionResponse(_, _, _, Some(true)) =>
            // $COVERAGE-OFF$
            Logger.info("[SubmissionController][post]:SubscriptionResponse(previouslySubmitted=true)")
            // $COVERAGE-ON$
            authenticator.refreshProfile map { _ =>
              Redirect(controllers.routes.LandingController.get())
            }
          case _ =>
            // $COVERAGE-OFF$
            Logger.info("[SubmissionController][post]:SubmissionResponse or SubscriptionResponse(previouslySubmitted=false)")
            // $COVERAGE-ON$
            Future.successful(Redirect(controllers.routes.ConfirmationController.get()))
        } recoverWith {
          case _: DuplicateEnrolmentException =>
            Logger.info("[SubmissionController][post] handling DuplicateEnrolmentException")
            Future.successful(Ok(duplicate_enrolment()))
          case e: DuplicateSubscriptionException =>
            Logger.info("[SubmissionController][post] handling DuplicateSubscriptionException")
            Future.successful(Ok(duplicate_submission(e.message)))
          case _: InvalidEnrolmentCredentialsException =>
            Logger.info("[SubmissionController][post] handling InvalidEnrolmentCredentialsException")
            Future.successful(Ok(wrong_credential_type()))
          case _: BadRequestException =>
            Logger.info("[SubmissionController][post] handling BadRequestException")
            Future.successful(Ok(bad_request()))
          case e: Exception =>
            Logger.info("[SubmissionController][post] handling Exception")
            throw e
        }
        case false =>
          // $COVERAGE-OFF$
          Logger.info("[SubmissionController][post]:false")
          // $COVERAGE-ON$
          Future.successful(Redirect(controllers.routes.RegistrationProgressController.get().url))
      }
  }

  private def subscribeBasedOnStatus(status: SubmissionStatus, groupIdentifier: Option[String], credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
                                    (implicit hc: HeaderCarrier, request: Request[_]) =
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