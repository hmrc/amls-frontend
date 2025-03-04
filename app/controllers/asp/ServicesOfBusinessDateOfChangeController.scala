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

package controllers.asp

import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.DateOfChangeFormProvider
import models.DateOfChange
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.asp.ServicesOfBusinessDateOfChangeService
import utils.{AuthAction, DateHelper}
import views.html.DateOfChangeView

import javax.inject.Inject
import scala.concurrent.Future

class ServicesOfBusinessDateOfChangeController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  service: ServicesOfBusinessDateOfChangeService,
  formProvider: DateOfChangeFormProvider,
  view: DateOfChangeView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction { implicit request =>
    Ok(getView(formProvider()))
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(getView(formWithErrors))),
        dateOfChange =>
          service.getModelWithDate(request.credId).flatMap {
            case (asp, Some(activityStartDate)) if !dateOfChange.dateOfChange.isBefore(activityStartDate.startDate) =>
              service.updateAsp(asp, dateOfChange, request.credId) map { _ =>
                Redirect(routes.SummaryController.get)
              }
            case (_, Some(activityStartDate))                                                                       =>
              Future.successful(
                BadRequest(
                  getView(
                    formProvider().withError(
                      "dateOfChange",
                      messages(
                        "error.expected.dateofchange.date.after.activitystartdate",
                        DateHelper.formatDate(activityStartDate.startDate)
                      )
                    )
                  )
                )
              )
            case (_, None)                                                                                          => Future.failed(new Exception("Could not retrieve start date"))
          }
      )
  }

  private def getView(form: Form[DateOfChange])(implicit request: Request[_]): Html = view(
    form,
    "summary.asp",
    routes.ServicesOfBusinessDateOfChangeController.post
  )
}
