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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.PSRNumberFormProvider
import models.flowmanagement.{AddBusinessTypeFlowModel, PsrNumberPageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.flowmanagement.Router
import utils.AuthAction
import views.html.businessmatching.updateservice.add.BusinessAppliedForPSRNumberView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BusinessAppliedForPSRNumberController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  val router: Router[AddBusinessTypeFlowModel],
  val cc: MessagesControllerComponents,
  formProvider: PSRNumberFormProvider,
  view: BusinessAppliedForPSRNumberView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key)) map {
      case model if model.isMsbTmDefined =>
        val form = model.businessAppliedForPSRNumber.fold(formProvider())(formProvider().fill)
        Ok(view(form, edit))
      case _                             => Redirect(controllers.routes.RegistrationProgressController.get())
    } getOrElse InternalServerError("Get: Unable to show Business Applied For PSR Number page")
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
            case Some(model) => model.businessAppliedForPSRNumber(data)
            case None        => throw new Exception("An UnknownException has occurred: BusinessAppliedForPSRNumberController")
          } flatMap {
            case Some(model) => router.getRoute(request.credId, PsrNumberPageId, model, edit)
            case _           =>
              Future
                .successful(InternalServerError("Post: Cannot retrieve data: BusinessAppliedForPSRNumberController"))
          }
      )
  }
}
