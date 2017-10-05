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
import models.businessactivities.{BusinessActivities, HowManyEmployees, EmployeeCount}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait HowManyEmployeesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def updateData(howManyEmployees: Option[HowManyEmployees], data: EmployeeCount): HowManyEmployees = {
    howManyEmployees.fold[HowManyEmployees](HowManyEmployees(employeeCount = Some(data.employeeCount)))(x =>
      x.copy(employeeCount = Some(data.employeeCount)))
  }

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
          response =>
            val form: Form2[HowManyEmployees] = (for {
              businessActivities <- response
              employees <- businessActivities.howManyEmployees
            } yield Form2[HowManyEmployees](employees)).getOrElse(EmptyForm)
            Ok(business_employees(form, edit))
        }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[EmployeeCount](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_employees(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.howManyEmployees(updateData(businessActivities.howManyEmployees, data)))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.TransactionRecordController.get())
          }
      }
    }
  }
}

object HowManyEmployeesController extends HowManyEmployeesController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val authConnector: AuthConnector = AMLSAuthConnector
}
