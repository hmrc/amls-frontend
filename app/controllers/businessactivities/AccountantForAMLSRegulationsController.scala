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

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import com.google.inject.Inject
import models.businessactivities.{AccountantForAMLSRegulations, BusinessActivities}
import utils.AuthAction
import views.html.businessactivities._

import scala.concurrent.Future

class AccountantForAMLSRegulationsController @Inject() (val dataCacheConnector: DataCacheConnector,
                                                        val authAction: AuthAction
                                                       ) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form: Form2[AccountantForAMLSRegulations] = (for {
            businessActivities <- response
            accountant <- businessActivities.accountantForAMLSRegulations
          } yield Form2[AccountantForAMLSRegulations](accountant)).getOrElse(EmptyForm)

          Ok(accountant_for_amls_regulations(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[AccountantForAMLSRegulations](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(accountant_for_amls_regulations(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId,BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key, updateModel(businessActivities, Some(data)))
          } yield (edit, data.accountantForAMLSRegulations) match {
            case (false, true) | (true, true) => Redirect(routes.WhoIsYourAccountantController.get())
            case _ => Redirect(routes.SummaryController.get())
          }
      }
    }
  }

  private def updateModel(ba: BusinessActivities, data: Option[AccountantForAMLSRegulations]): BusinessActivities = {
    data match {
      case d@Some(AccountantForAMLSRegulations(true)) => ba.accountantForAMLSRegulations(d)
      case d@Some(AccountantForAMLSRegulations(false)) => ba.accountantForAMLSRegulations(d).whoIsYourAccountant(None).taxMatters(None)
    }
  }
}
