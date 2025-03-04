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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import controllers.{AmlsBaseController, CommonPlayDependencies}

import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivities, BusinessActivity}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import utils.AuthAction
import utils.businessmatching.CheckYourAnswersHelper
import views.html.businessmatching.CheckYourAnswersView

import scala.concurrent.Future

@Singleton
class SummaryController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val businessMatchingService: BusinessMatchingService,
  cyaHelper: CheckYourAnswersHelper,
  val cc: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      bm     <- businessMatchingService.getModel(request.credId)
      ba     <- OptionT.fromOption[Future](bm.activities)
      status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
    } yield {
      val isPreSubmission            = statusService.isPreSubmission(status)
      val bmWithAdditionalActivities = bm.copy(
        activities = Some(
          BusinessActivities(
            ba.businessActivities ++ ba.additionalActivities.fold[Set[BusinessActivity]](Set.empty)(act => act)
          )
        )
      )

      val summaryList = cyaHelper.createSummaryList(
        bmWithAdditionalActivities,
        isPreSubmission,
        statusService.isPending(status)
      )

      val submissionButton = cyaHelper.getSubmitButton(
        bmWithAdditionalActivities.businessAppliedForPSRNumber,
        isPreSubmission,
        bmWithAdditionalActivities.preAppComplete
      )

      Ok(view(summaryList, submissionButton, isPreSubmission))

    }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    {
      for {
        businessMatching <- businessMatchingService.getModel(request.credId)
        _                <- businessMatchingService.updateModel(
                              request.credId,
                              businessMatching.copy(hasAccepted = true, preAppComplete = true)
                            )
      } yield Redirect(controllers.routes.RegistrationProgressController.get())
    } getOrElse InternalServerError("Unable to update business matching")
  }
}
