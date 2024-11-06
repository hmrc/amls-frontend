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

package controllers

import exceptions.{DuplicateEnrolmentException, DuplicateSubscriptionException, InvalidEnrolmentCredentialsException}
import models.status._
import models.{SubmissionResponse, SubscriptionResponse}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{RenewalService, SectionsProvider, StatusService, SubmissionService}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.{AuthAction, DeclarationHelper}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SubmissionController @Inject()(val subscriptionService: SubmissionService,
                                     val statusService: StatusService,
                                     val renewalService: RenewalService,
                                     authAction: AuthAction,
                                     val ds: CommonPlayDependencies,
                                     val cc: MessagesControllerComponents,
                                     val sectionsProvider: SectionsProvider) extends AmlsBaseController(ds, cc) with Logging {

  private def handleRenewalAmendment(credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
                                    (implicit headerCarrier: HeaderCarrier): Future[SubmissionResponse] = {

    renewalService.getRenewal(credId) flatMap {
      case Some(renewal) => subscriptionService.renewalAmendment(credId, amlsRegistrationNumber, accountTypeId, renewal)
      case _ => subscriptionService.variation(credId, amlsRegistrationNumber, accountTypeId)
    }
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    lazy val whenSectionsComplete = {
      logger.info("[SubmissionController][post]:true")
      statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId).flatMap[SubmissionResponse](status =>
        subscribeBasedOnStatus(status, request.groupIdentifier, request.credId, request.amlsRefNumber, request.accountTypeId))
    }.flatMap {
      case SubscriptionResponse(_, _, _, Some(true)) =>
        logger.info("[SubmissionController][post]:SubscriptionResponse(previouslySubmitted=true)")
        Future.successful(Redirect(controllers.routes.LandingController.get()))
      case _ =>
        logger.info("[SubmissionController][post]:SubmissionResponse or SubscriptionResponse(previouslySubmitted=false)")
        Future.successful(Redirect(controllers.routes.ConfirmationController.get()))
    } recoverWith {
      case _: DuplicateEnrolmentException =>
        logger.warn("[SubmissionController][post] handling DuplicateEnrolmentException")
        Future.successful(Redirect(routes.SubmissionErrorController.duplicateEnrolment()))
      case e: DuplicateSubscriptionException =>
        logger.warn("[SubmissionController][post] handling DuplicateSubscriptionException")
        Future.successful(Redirect(routes.SubmissionErrorController.duplicateSubmission()))
      case _: InvalidEnrolmentCredentialsException =>
        logger.warn("[SubmissionController][post] handling InvalidEnrolmentCredentialsException")
        Future.successful(Redirect(routes.SubmissionErrorController.wrongCredentialType()))
      case _: BadRequestException =>
        logger.warn("[SubmissionController][post] handling BadRequestException")
        Future.successful(Redirect(routes.SubmissionErrorController.badRequest()))
      case e: Exception =>
        logger.warn("[SubmissionController][post] handling Exception")
        throw e
    }

    for {
      isRenewal <- renewalService.isRenewalFlow(request.amlsRefNumber, request.accountTypeId, request.credId)
      sectionsComplete <- DeclarationHelper.sectionsComplete(request.credId, sectionsProvider, isRenewal)
      result <- sectionsComplete match {
        case true => whenSectionsComplete
        case false =>
          logger.info("sections aren't complete, redirecting")
          Future.successful(Redirect(controllers.routes.RegistrationProgressController.get().url))
      }
    } yield result
  }

  private def subscribeBasedOnStatus(status: SubmissionStatus, groupIdentifier: Option[String], credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
                                    (implicit hc: HeaderCarrier): Future[SubmissionResponse] =
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