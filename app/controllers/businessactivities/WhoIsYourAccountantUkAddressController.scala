/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.businessactivities.AccountantUKAddressFormProvider
import models.businessactivities.BusinessActivities
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities.AccountantUKAddressView

class WhoIsYourAccountantUkAddressController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val autoCompleteService: AutoCompleteService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: AccountantUKAddressFormProvider,
  view: AccountantUKAddressView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { response =>
      val form = (for {
        businessActivities  <- response
        whoIsYourAccountant <- businessActivities.whoIsYourAccountant.flatMap(acc => acc.address)
      } yield if (whoIsYourAccountant.isUk) formProvider().fill(whoIsYourAccountant) else formProvider())
        .getOrElse(formProvider())
      Ok(view(form, edit, ControllerHelper.accountantName(response)))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithError =>
          dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { response =>
            BadRequest(view(formWithError, edit, ControllerHelper.accountantName(response)))
          },
        data =>
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _                <- dataCacheConnector.save[BusinessActivities](
                                  request.credId,
                                  BusinessActivities.key,
                                  businessActivity.whoIsYourAccountant(
                                    businessActivity.flatMap(ba => ba.whoIsYourAccountant).map(acc => acc.copy(address = Option(data)))
                                  )
                                )
          } yield
            if (edit) {
              Redirect(routes.SummaryController.get)
            } else {
              Redirect(routes.TaxMattersController.get())
            }
      )
  }
}
