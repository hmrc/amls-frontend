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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, EmployeeCountAMLSSupervision, HowManyEmployees}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import views.html.businessactivities._

import scala.concurrent.Future

class EmployeeCountAMLSSupervisionController @Inject() (val dataCacheConnector: DataCacheConnector,
                                                        val authAction: AuthAction,
                                                        val ds: CommonPlayDependencies,
                                                        val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def updateData(howManyEmployees: Option[HowManyEmployees], data: EmployeeCountAMLSSupervision): HowManyEmployees = {
    howManyEmployees.fold[HowManyEmployees](HowManyEmployees(employeeCountAMLSSupervision = Some(data.employeeCountAMLSSupervision)))(x =>
      x.copy(employeeCountAMLSSupervision = Some(data.employeeCountAMLSSupervision)))
  }

  def get(edit: Boolean = false) = authAction.async {
      implicit request => {
        dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
          response =>
            val form: Form2[HowManyEmployees] = (for {
              businessActivities <- response
              employees <- businessActivities.howManyEmployees
            } yield Form2[HowManyEmployees](employees)).getOrElse(EmptyForm)
            Ok(business_employees_amls_supervision(form, edit))
        }
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[EmployeeCountAMLSSupervision](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_employees_amls_supervision(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivities.howManyEmployees(updateData(businessActivities.howManyEmployees, data)))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.HowManyEmployeesController.get())
          }
      }
    }
  }
}
