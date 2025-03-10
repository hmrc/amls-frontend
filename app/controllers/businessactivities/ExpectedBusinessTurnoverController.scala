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
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.ExpectedBusinessTurnoverFormProvider
import models.businessactivities._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import utils.AuthAction
import views.html.businessactivities.ExpectedBusinessTurnoverView

import scala.concurrent.Future

class ExpectedBusinessTurnoverController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  implicit val statusService: StatusService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: ExpectedBusinessTurnoverFormProvider,
  view: ExpectedBusinessTurnoverView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { response =>
      val form = response.expectedBusinessTurnover.fold(formProvider())(formProvider().fill)
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(view(formWithError, edit))),
        data =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _                  <- dataCacheConnector.save[BusinessActivities](
                                    request.credId,
                                    BusinessActivities.key,
                                    businessActivities.expectedBusinessTurnover(data)
                                  )
          } yield
            if (edit) {
              Redirect(routes.SummaryController.get)
            } else {
              Redirect(routes.ExpectedAMLSTurnoverController.get())
            }
      )
  }
}
