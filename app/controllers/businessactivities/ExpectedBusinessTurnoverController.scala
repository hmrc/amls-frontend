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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities._
import services.StatusService
import utils.ControllerHelper
import views.html.businessactivities._

import scala.concurrent.Future

trait ExpectedBusinessTurnoverController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  implicit val statusService: StatusService

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true => dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
          response =>
            val form: Form2[ExpectedBusinessTurnover] = (for {
              businessActivities <- response
              expectedTurnover <- businessActivities.expectedBusinessTurnover
            } yield Form2[ExpectedBusinessTurnover](expectedTurnover)).getOrElse(EmptyForm)
            Ok(expected_business_turnover(form, edit))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedBusinessTurnover](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(expected_business_turnover(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.expectedBusinessTurnover(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.ExpectedAMLSTurnoverController.get())
          }
      }
    }
  }
}

object ExpectedBusinessTurnoverController extends ExpectedBusinessTurnoverController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService: StatusService = StatusService
}
