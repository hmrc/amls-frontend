/*
 * Copyright 2019 HM Revenue & Customs
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

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessactivities.{BusinessActivities, _}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import views.html.businessactivities._

import scala.concurrent.Future

class BusinessFranchiseController @Inject() (val dataCacheConnector: DataCacheConnector,
                                             val authAction: AuthAction,
                                             val ds: CommonPlayDependencies,
                                             val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
   implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form: Form2[BusinessFranchise] = (for {
            businessActivities <- response
            businessFranchise <- businessActivities.businessFranchise
          } yield Form2[BusinessFranchise](businessFranchise)).getOrElse(EmptyForm)
          Ok(business_franchise_name(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[BusinessFranchise](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_franchise_name(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivities.businessFranchise(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.EmployeeCountAMLSSupervisionController.get())
          }
      }
    }
  }
}
