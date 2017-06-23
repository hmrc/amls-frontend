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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.ControllerHelper

import scala.concurrent.Future

trait DetailedAnswersController extends BaseController {

  protected def dataCache: DataCacheConnector
  protected def statusService: StatusService

  private def showHideAddressMove(lineId: Option[Int])(implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_) if lineId.isDefined => true
      case _ => false
    }
  }

  def get(index: Int, fromYourAnswers: Boolean) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCache.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) flatMap {
          case Some(data) => {
            data.lift(index - 1) match {
              case Some(x) => showHideAddressMove(x.lineId) map {showHide =>
                Ok(views.html.responsiblepeople.detailed_answers(Some(x), index, fromYourAnswers, showHide, ControllerHelper.rpTitleName(Some(x))))
              }
              case _ => Future.successful(NotFound(notFoundView))
            }
          }
          case _ => Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
        }
    }
}

object DetailedAnswersController extends DetailedAnswersController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override protected def statusService: StatusService = StatusService
}
