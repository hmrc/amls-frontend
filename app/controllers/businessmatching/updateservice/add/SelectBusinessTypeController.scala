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
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.updateservice.add.SelectActivitiesFormProvider
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, BusinessActivity}
import models.flowmanagement.{AddBusinessTypeFlowModel, SelectBusinessTypesPageId}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching.updateservice.add.SelectActivitiesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SelectBusinessTypeController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  val businessMatchingService: BusinessMatchingService,
  val router: Router[AddBusinessTypeFlowModel],
  val addHelper: AddBusinessTypeHelper,
  val cc: MessagesControllerComponents,
  formProvider: SelectActivitiesFormProvider,
  view: SelectActivitiesView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      model  <- OptionT(
                  dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key)(
                    model => model.getOrElse(AddBusinessTypeFlowModel())
                  )
                )
      values <- getFormData(request.credId)
    } yield {
      val form = model.activity.fold(formProvider())(formProvider().fill)
      Ok(view(form, edit, values))
    }) getOrElse InternalServerError("Get: Unable to show Select Activities page. Failed to retrieve data")
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          getFormData(request.credId) map { values =>
            BadRequest(view(formWithErrors, edit, values))
          } getOrElse InternalServerError("Post: Invalid form on Select Activities page"),
        data =>
          dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
            _.getOrElse(AddBusinessTypeFlowModel()).activity(data)
          } flatMap {
            case Some(model) => router.getRoute(request.credId, SelectBusinessTypesPageId, model, edit)
            case _           => Future.successful(InternalServerError("Post: Cannot retrieve data: SelectActivitiesController"))
          }
      )
  }

  private def getFormData(credId: String)(implicit messages: Messages): OptionT[Future, Seq[BusinessActivity]] = for {
    model      <- businessMatchingService.getModel(credId)
    activities <- OptionT.fromOption[Future](model.activities) map {
                    _.businessActivities
                  }
  } yield (BusinessMatchingActivities.all diff activities).toSeq.sortBy(_.getMessage(true))
}
