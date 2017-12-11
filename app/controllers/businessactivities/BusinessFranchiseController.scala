/*
 * Copyright 2017 HM Revenue & Customs
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
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessactivities.{BusinessActivities, _}
import views.html.businessactivities._

import scala.concurrent.Future

trait BusinessFranchiseController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[BusinessFranchise] = (for {
            businessActivities <- response
            businessFranchise <- businessActivities.businessFranchise
          } yield Form2[BusinessFranchise](businessFranchise)).getOrElse(EmptyForm)
          Ok(business_franchise_name(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BusinessFranchise](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_franchise_name(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
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

object BusinessFranchiseController extends BusinessFranchiseController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
