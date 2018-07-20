/*
 * Copyright 2018 HM Revenue & Customs
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

import config.AMLSAuthConnector
import exceptions.{DuplicateEnrolmentException, DuplicateSubscriptionException, InvalidEnrolmentCredentialsException}
import javax.inject.{Inject, Singleton}
import models.status._
import models.{SubmissionResponse, SubscriptionResponse}
import play.api.Logger
import services.{RenewalService, StatusService, SubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubmissionController @Inject()(
                                     val subscriptionService: SubmissionService,
                                     val statusService: StatusService,
                                     val renewalService: RenewalService,
                                     val authConnector: AuthConnector = AMLSAuthConnector
                                    ) extends BaseController {

  private def handleRenewalAmendment()(implicit authContext: AuthContext, headerCarrier: HeaderCarrier) = {
    renewalService.getRenewal flatMap {
      case Some(r) => subscriptionService.renewalAmendment(r)
      case _ => subscriptionService.variation
    }
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request => {
        statusService.getStatus.flatMap[SubmissionResponse](subscribeBasedOnStatus)
      }.flatMap {
        case SubscriptionResponse(_, _, _, Some(true)) => Future.successful(Redirect(controllers.routes.LandingController.get()))
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
      }
  }

  private def subscribeBasedOnStatus(status: SubmissionStatus)(implicit hc: HeaderCarrier, ac: AuthContext) = status match {
    case SubmissionReadyForReview => subscriptionService.update
    case SubmissionDecisionApproved => subscriptionService.variation
    case ReadyForRenewal(_) => renewalService.getRenewal flatMap {
      case Some(r) => subscriptionService.renewal(r)
      case _ => subscriptionService.variation
    }
    case RenewalSubmitted(_) => handleRenewalAmendment()
    case _ => subscriptionService.subscribe
  }
}

//object SubmissionController extends SubmissionController {
//  // $COVERAGE-OFF$
//  override protected def authConnector: AuthConnector = AMLSAuthConnector
//
//  override private[controllers] val renewalService = Play.current.injector.instanceOf[RenewalService]
//  override private[controllers] lazy val subscriptionService = Play.current.injector.instanceOf[SubmissionService]
//  override private[controllers] val statusService: StatusService = StatusService
//  override private[controllers] lazy val authenticator = Play.current.injector.instanceOf[AuthenticatorConnector]
//}
