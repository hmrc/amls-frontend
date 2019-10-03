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
import forms.{Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, UkAccountantsAddress, WhoIsYourAccountant}
import play.api.mvc.MessagesControllerComponents
import services.AutoCompleteService
import utils.AuthAction

import scala.concurrent.Future

class WhoIsYourAccountantController @Inject() ( val dataCacheConnector: DataCacheConnector,
                                                val autoCompleteService: AutoCompleteService,
                                                val authAction: AuthAction,
                                                val ds: CommonPlayDependencies,
                                                val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  //Joe - cannot seem to provide a default for UK/Non UK without providing defaults for other co-products
  private val defaultValues = WhoIsYourAccountant("", None, UkAccountantsAddress("","", None, None, ""))

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            whoIsYourAccountant <- businessActivities.whoIsYourAccountant
          } yield {
            Form2[WhoIsYourAccountant](whoIsYourAccountant)
          }).getOrElse(Form2(defaultValues))
          Ok(views.html.businessactivities.who_is_your_accountant(form, edit, autoCompleteService.getCountries))
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[WhoIsYourAccountant](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.businessactivities.who_is_your_accountant(f, edit, autoCompleteService.getCountries)))
        case ValidForm(_, data) => {
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivity.whoIsYourAccountant(Some(data))
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
