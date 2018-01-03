/*
 * Copyright 2018 HM Revenue & Customs
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
import models.businessactivities.{BusinessActivities, EmployeeCount, EmployeeCountAMLSSupervision, HowManyEmployees}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait EmployeeCountAMLSSupervisionController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def updateData(howManyEmployees: Option[HowManyEmployees], data: EmployeeCountAMLSSupervision): HowManyEmployees = {
    howManyEmployees.fold[HowManyEmployees](HowManyEmployees(employeeCountAMLSSupervision = Some(data.employeeCountAMLSSupervision)))(x =>
      x.copy(employeeCountAMLSSupervision = Some(data.employeeCountAMLSSupervision)))
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
            Ok(business_employees_amls_supervision(form, edit))
        }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[EmployeeCountAMLSSupervision](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_employees_amls_supervision(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.howManyEmployees(updateData(businessActivities.howManyEmployees, data)))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.HowManyEmployeesController.get())
          }
      }
    }
  }
}

object EmployeeCountAMLSSupervisionController extends EmployeeCountAMLSSupervisionController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val authConnector: AuthConnector = AMLSAuthConnector
}
