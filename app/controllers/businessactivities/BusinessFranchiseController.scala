/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.businessactivities.BusinessFranchiseFormProvider
import models.businessactivities.BusinessActivities
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.businessactivities.BusinessFranchiseNameView

import scala.concurrent.Future

class BusinessFranchiseController @Inject() (val dataCacheConnector: DataCacheConnector,
                                             val authAction: AuthAction,
                                             val ds: CommonPlayDependencies,
                                             val cc: MessagesControllerComponents,
                                             formProvider: BusinessFranchiseFormProvider,
                                             view: BusinessFranchiseNameView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
   implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            businessFranchise <- businessActivities.businessFranchise
          } yield formProvider().fill(businessFranchise)).getOrElse(formProvider())
          Ok(view(form, edit))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request => {
      formProvider().bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivities.businessFranchise(data)
            )
          } yield if (edit) {
            Redirect(routes.SummaryController.get)
          } else {
            Redirect(routes.EmployeeCountAMLSSupervisionController.get())
          }
      )
    }
  }
}
