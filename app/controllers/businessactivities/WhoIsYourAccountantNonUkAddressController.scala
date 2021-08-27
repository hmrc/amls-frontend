/*
 * Copyright 2021 HM Revenue & Customs
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
import models.businessactivities.{AccountantsAddress, BusinessActivities, NonUkAccountantsAddress}
import play.api.mvc.MessagesControllerComponents
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities.who_is_your_accountant_non_uk_address



class WhoIsYourAccountantNonUkAddressController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                          val autoCompleteService: AutoCompleteService,
                                                          val authAction: AuthAction,
                                                          val ds: CommonPlayDependencies,
                                                          val cc: MessagesControllerComponents,
                                                          who_is_your_accountant_non_uk_address: who_is_your_accountant_non_uk_address) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            whoIsYourAccountant <- businessActivities.whoIsYourAccountant.flatMap(acc => acc.address)
          } yield {
            if(!whoIsYourAccountant.isUk) { Form2[AccountantsAddress](whoIsYourAccountant) } else { EmptyForm }
          }).getOrElse(EmptyForm)
          Ok(who_is_your_accountant_non_uk_address(form, edit, ControllerHelper.accountantName(response), autoCompleteService.getCountries))
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[NonUkAccountantsAddress](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
            response => BadRequest(who_is_your_accountant_non_uk_address(
              f, edit, ControllerHelper.accountantName(response), autoCompleteService.getCountries))
          }
        case ValidForm(_, data) => {
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivity.whoIsYourAccountant(businessActivity.flatMap(ba => ba.whoIsYourAccountant).map(acc => acc.copy(address = Option(data))))
            )
          } yield if (edit) {
            Redirect(routes.SummaryController.get)
          } else {
            Redirect(routes.TaxMattersController.get())
          }
        }
      }
  }
}
