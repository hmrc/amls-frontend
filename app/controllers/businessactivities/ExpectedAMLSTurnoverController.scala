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

package controllers.businessactivities

import com.google.inject.Inject
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.ExpectedAMLSTurnoverFormProvider
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.StatusService
import services.businessactivities.ExpectedAMLSTurnoverService
import utils.AuthAction
import views.html.businessactivities.ExpectedAMLSTurnoverView

import scala.concurrent.Future

class ExpectedAMLSTurnoverController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val statusService: StatusService,
  val cc: MessagesControllerComponents,
  service: ExpectedAMLSTurnoverService,
  formProvider: ExpectedAMLSTurnoverFormProvider,
  view: ExpectedAMLSTurnoverView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    service.getBusinessMatchingExpectedTurnover(request.credId) map {
      case Some((bm, Some(turnover))) =>
        Ok(view(formProvider().fill(turnover), edit, bm, bm.alphabeticalBusinessActivitiesLowerCase()))
      case Some((bm, _))              =>
        Ok(view(formProvider(), edit, bm, bm.alphabeticalBusinessActivitiesLowerCase()))
      case _                          =>
        Ok(view(formProvider(), edit, None, None))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    service.getBusinessMatching(request.credId) flatMap { bm =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, edit, bm, bm.alphabeticalBusinessActivitiesLowerCase()))),
          data => service.updateBusinessActivities(request.credId, data).map(_ => redirectLogic(edit))
        )
    }
  }

  private def redirectLogic(edit: Boolean): Result =
    if (edit) {
      Redirect(routes.SummaryController.get)
    } else {
      Redirect(routes.BusinessFranchiseController.get())
    }
}
