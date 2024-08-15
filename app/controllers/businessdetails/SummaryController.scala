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

package controllers.businessdetails

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessdetails.BusinessDetails
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.AuthAction
import utils.businessdetails.CheckYourAnswersHelper
import views.html.businessdetails.CheckYourAnswersView


class SummaryController @Inject () (
                                     val dataCache: DataCacheConnector,
                                     val statusService: StatusService,
                                     val authAction: AuthAction,
                                     val ds: CommonPlayDependencies,
                                     val cc: MessagesControllerComponents,
                                     cyaHelper: CheckYourAnswersHelper,
                                     view: CheckYourAnswersView) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async {
    implicit request =>
      for {
        businessDetails <- dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key)
        status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
      } yield businessDetails match {
        case Some(data) => {
          val showRegisteredForMLR = status match {
            case NotCompleted | SubmissionReady | SubmissionReadyForReview => true
            case _ => false
          }
          val summaryListRows: SummaryList = cyaHelper.createSummaryList(data, showRegisteredForMLR)
          Ok(view(summaryListRows))
        }
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def post: Action[AnyContent] = authAction.async {
    implicit request =>
      for {
        businessDetails <- dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key)
        _ <- dataCache.save[BusinessDetails](request.credId, BusinessDetails.key,
          businessDetails.copy(hasAccepted = true)
        )
      } yield {
        Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}
