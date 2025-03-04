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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.DateOfChangeFormProvider
import models.flowmanagement._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.flowmanagement.Router
import utils.AuthAction
import views.html.DateOfChangeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class WhatDateRemovedController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val router: Router[RemoveBusinessTypeFlowModel],
  val cc: MessagesControllerComponents,
  formProvider: DateOfChangeFormProvider,
  view: DateOfChangeView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    getFormData(request.credId) map { model =>
      val form = model.dateOfChange.fold(formProvider())(formProvider().fill)
      Ok(view(form, "summary.updateservice", routes.WhatDateRemovedController.post(edit), false, "button.continue"))
    } getOrElse InternalServerError("Get: Unable to show date_of_change Activities page. Failed to retrieve data")
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                "summary.updateservice",
                routes.WhatDateRemovedController.post(edit),
                false,
                "button.continue"
              )
            )
          ),
        data =>
          dataCacheConnector.update[RemoveBusinessTypeFlowModel](request.credId, RemoveBusinessTypeFlowModel.key) {
            case Some(model) => model.copy(dateOfChange = Some(data))
            case None        => throw new Exception("An UnknownException has occurred: UpdateServiceDateofChangeController")
          } flatMap {
            case Some(model) => router.getRoute(request.credId, WhatDateRemovedPageId, model, edit)
            case _           =>
              Future.successful(InternalServerError("Post: Cannot retrieve data: UpdateServiceDateofChangeController"))
          }
      )
  }

  private def getFormData(credId: String): OptionT[Future, RemoveBusinessTypeFlowModel] = for {
    model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](credId, RemoveBusinessTypeFlowModel.key))
  } yield model

}
