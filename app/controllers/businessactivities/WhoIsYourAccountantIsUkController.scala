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
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, WhoIsYourAccountant, WhoIsYourAccountantIsUk}
import services.AutoCompleteService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthAction, ControllerHelper}

class WhoIsYourAccountantIsUkController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                  val autoCompleteService: AutoCompleteService,
                                                  val authAction: AuthAction ) extends DefaultBaseController  {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            isUk <- businessActivities.whoIsYourAccountant.flatMap(acc => acc.isUk)
          } yield {
            Form2[WhoIsYourAccountantIsUk](isUk)
          }).getOrElse(EmptyForm)
          Ok(views.html.businessactivities.who_is_your_accountant_is_uk_address(form, edit, ControllerHelper.accountantName(response)))
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[WhoIsYourAccountantIsUk](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
            response => BadRequest(views.html.businessactivities.who_is_your_accountant_is_uk_address(f, edit, ControllerHelper.accountantName(response)))
          }
        case ValidForm(_, data) =>
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key, updateModel(businessActivity, data))
          } yield if (data.isUk) {
            Redirect(routes.WhoIsYourAccountantUkAddressController.get(edit))
          } else {
            Redirect(routes.WhoIsYourAccountantNonUkAddressController.get(edit))
          }
      }
  }

  private def updateModel(ba: BusinessActivities, data: WhoIsYourAccountantIsUk): BusinessActivities = {
    ba.copy(whoIsYourAccountant = ba.whoIsYourAccountant.map(accountant =>
      if(changedIsUk(accountant, data)) {
        accountant.isUk(data).address(None)
      } else {
        accountant.isUk(data)
      })
    )
  }

  private def changedIsUk(accountant: WhoIsYourAccountant, newData: WhoIsYourAccountantIsUk): Boolean =
    accountant.address.map(add => add.isUk).exists(isUk => isUk != newData.isUk)
}
