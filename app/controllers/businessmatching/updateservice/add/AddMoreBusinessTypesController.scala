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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.updateservice.add.AddMoreActivitiesFormProvider
import models.flowmanagement.{AddBusinessTypeFlowModel, AddMoreBusinessTypesPageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.flowmanagement.Router
import utils.AuthAction
import views.html.businessmatching.updateservice.add.AddMoreActivitiesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AddMoreBusinessTypesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  val router: Router[AddBusinessTypeFlowModel],
  val cc: MessagesControllerComponents,
  formProvider: AddMoreActivitiesFormProvider,
  view: AddMoreActivitiesView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction { implicit request =>
    Ok(view(formProvider()))
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        data =>
          dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
            case Some(model) => model.copy(addMoreActivities = Some(data))
            case None        => throw new Exception("An UnknownException has occurred: AddMoreActivitiesController")
          } flatMap {
            case Some(model) => router.getRoute(request.credId, AddMoreBusinessTypesPageId, model)
            case _           => Future.successful(InternalServerError("Post: Cannot retrieve data: AddMoreActivitiesController"))
          }
      )
  }
}
