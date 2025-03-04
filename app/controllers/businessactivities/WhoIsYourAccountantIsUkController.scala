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
import forms.businessactivities.AccountantIsUKAddressFormProvider
import models.businessactivities.{BusinessActivities, WhoIsYourAccountant, WhoIsYourAccountantIsUk}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities.AccountantIsUKAddressView

class WhoIsYourAccountantIsUkController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val autoCompleteService: AutoCompleteService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: AccountantIsUKAddressFormProvider,
  view: AccountantIsUKAddressView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { response =>
      val form = (for {
        businessActivities <- response
        isUk               <- businessActivities.whoIsYourAccountant.flatMap(acc => acc.isUk)
      } yield formProvider().fill(isUk)).getOrElse(formProvider())
      Ok(view(form, edit, ControllerHelper.accountantName(response)))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { response =>
            BadRequest(view(formWithErrors, edit, ControllerHelper.accountantName(response)))
          },
        data =>
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _                <-
              dataCacheConnector
                .save[BusinessActivities](request.credId, BusinessActivities.key, updateModel(businessActivity, data))
          } yield
            if (data.isUk) {
              Redirect(routes.WhoIsYourAccountantUkAddressController.get(edit))
            } else {
              Redirect(routes.WhoIsYourAccountantNonUkAddressController.get(edit))
            }
      )
  }

  private def updateModel(ba: BusinessActivities, data: WhoIsYourAccountantIsUk): BusinessActivities =
    ba.copy(whoIsYourAccountant =
      ba.whoIsYourAccountant.map(accountant =>
        if (changedIsUk(accountant, data)) {
          accountant.isUk(data).address(None)
        } else {
          accountant.isUk(data)
        }
      )
    )

  private def changedIsUk(accountant: WhoIsYourAccountant, newData: WhoIsYourAccountantIsUk): Boolean =
    accountant.address.map(add => add.isUk).exists(isUk => isUk != newData.isUk)
}
