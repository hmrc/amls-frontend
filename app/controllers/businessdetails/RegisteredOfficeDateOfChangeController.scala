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
import forms.DateOfChangeFormProvider
import models.DateOfChange
import models.businessdetails.{BusinessDetails, RegisteredOfficeNonUK, RegisteredOfficeUK}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.StatusService
import utils.{AuthAction, DateHelper, DateOfChangeHelper}
import views.html.DateOfChangeView

import scala.concurrent.Future

class RegisteredOfficeDateOfChangeController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: DateOfChangeFormProvider,
  view: DateOfChangeView
) extends AmlsBaseController(ds, cc)
    with DateOfChangeHelper {

  def get: Action[AnyContent] = authAction { implicit request =>
    Ok(getView(formProvider()))
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(getView(formWithErrors))),
        doc =>
          dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key).flatMap { details =>
            details.activityStartDate match {
              case Some(date) if !doc.dateOfChange.isBefore(date.startDate) =>
                dataCacheConnector.save[BusinessDetails](
                  request.credId,
                  BusinessDetails.key,
                  details.registeredOffice(details.registeredOffice match {
                    case Some(office: RegisteredOfficeUK)    => office.copy(dateOfChange = Some(doc))
                    case Some(office: RegisteredOfficeNonUK) => office.copy(dateOfChange = Some(doc))
                    case _                                   => throw new Exception("An exception has occurred")
                  })
                ) map { _ =>
                  Redirect(routes.SummaryController.get)
                }
              case Some(date)                                               =>
                Future.successful(
                  BadRequest(
                    getView(
                      formProvider().withError(
                        "dateOfChange",
                        messages(
                          "error.expected.dateofchange.date.after.activitystartdate",
                          DateHelper.formatDate(date.startDate)
                        )
                      )
                    )
                  )
                )
              case None                                                     =>
                Future.failed(new Exception("Could not retrieve start date"))
            }
          }
      )
  }

  private def getView(form: Form[DateOfChange])(implicit request: Request[_]): Html = view(
    form,
    "summary.businessdetails",
    controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
  )
}
