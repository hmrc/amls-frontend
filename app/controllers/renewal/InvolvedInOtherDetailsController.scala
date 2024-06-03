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

package controllers.renewal

import config.AmlsErrorHandler
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.InvolvedInOtherDetailsFormProvider
import models.renewal.InvolvedInOtherYes
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.RenewalService
import services.RenewalService.BusinessAndOtherActivities
import utils.AuthAction
import utils.CharacterCountParser.cleanData
import views.html.renewal.InvolvedInOtherDetailsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class InvolvedInOtherDetailsController @Inject()(
                                                  authAction: AuthAction,
                                                  commonPlayDeps: CommonPlayDependencies,
                                                  messagesComps: MessagesControllerComponents,
                                                  formProvider: InvolvedInOtherDetailsFormProvider,
                                                  view: InvolvedInOtherDetailsView,
                                                  renewalService: RenewalService,
                                                  errorHandler: AmlsErrorHandler) extends AmlsBaseController(commonPlayDeps, messagesComps) with Logging {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    renewalService.getRenewal(request.credId).map {
      case Some(renewal) => Ok(view(formProvider().fill(renewal.involvedInOtherActivities.head.asInstanceOf[InvolvedInOtherYes]), edit))
      case None => InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider().bindFromRequest(cleanData(request.body, "details")).fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
      involvedInOtherYes => renewalService.updateOtherBusinessActivities(request.credId, involvedInOtherYes)
        .map(optBusinessAndOtherActivities => redirect(optBusinessAndOtherActivities, edit, request.credId))
    )
  }

  private def redirect(optBusinessAndOtherActivities: Option[BusinessAndOtherActivities], edit: Boolean, credId: String)
                      (implicit request: Request[_]): Result = {
    optBusinessAndOtherActivities match {
      case Some(_) => Redirect(routes.BusinessTurnoverController.get(edit))
      case None => {
        logger.error(s"Unable to fetch business activities or other activities for $credId")
        InternalServerError(errorHandler.internalServerErrorTemplate)
      }
    }
  }
}
