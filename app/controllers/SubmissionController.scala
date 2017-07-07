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

package controllers

import config.AMLSAuthConnector
import connectors.AuthenticatorConnector
import models.{SubmissionResponse, SubscriptionResponse}
import models.status._
import play.api.Play
import services.{RenewalService, StatusService, SubmissionService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}
import views.html.duplicate_submission

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SubmissionController extends BaseController {

  private[controllers] def subscriptionService: SubmissionService

  private[controllers] def statusService: StatusService

  private[controllers] def renewalService: RenewalService

  private[controllers] def authenticator: AuthenticatorConnector

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
        case SubscriptionResponse(_, _, _, Some(true)) =>
          authenticator.refreshProfile map { _ =>
            Redirect(controllers.routes.LandingController.get())
          }
        case _ => Future.successful(Redirect(controllers.routes.ConfirmationController.get()))
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

object SubmissionController extends SubmissionController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector

  override private[controllers] val renewalService = Play.current.injector.instanceOf[RenewalService]
  override private[controllers] val subscriptionService: SubmissionService = SubmissionService
  override private[controllers] val statusService: StatusService = StatusService
  override private[controllers] lazy val authenticator = Play.current.injector.instanceOf[AuthenticatorConnector]
}
