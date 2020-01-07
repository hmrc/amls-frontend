/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{AccountantsAddress, BusinessActivities, UkAccountantsAddress}
import services.AutoCompleteService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthAction, ControllerHelper}

class WhoIsYourAccountantUkAddressController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                       val autoCompleteService: AutoCompleteService,
                                                       val authAction: AuthAction) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            whoIsYourAccountant <- businessActivities.whoIsYourAccountant.flatMap(acc => acc.address)
          } yield {
            if(whoIsYourAccountant.isUk) { Form2[AccountantsAddress](whoIsYourAccountant) } else { EmptyForm }
          }).getOrElse(EmptyForm)
          Ok(views.html.businessactivities.who_is_your_accountant_uk_address(form, edit, ControllerHelper.accountantName(response)))
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[UkAccountantsAddress](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
            response => BadRequest(views.html.businessactivities.who_is_your_accountant_uk_address(f, edit, ControllerHelper.accountantName(response)))
          }
        case ValidForm(_, data) => {
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivity.whoIsYourAccountant(businessActivity.flatMap(ba => ba.whoIsYourAccountant).map(acc => acc.copy(address = Option(data))))
            )
          } yield if (edit) {
            Redirect(routes.SummaryController.get())
          } else {
            Redirect(routes.TaxMattersController.get())
          }
        }
      }
  }
}
