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

package controllers.bankdetails

import connectors.DataCacheConnector
import controllers.CommonPlayDependencies
import javax.inject.{Inject, Singleton}
import models.bankdetails.BankDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, StatusConstants}
import views.html.bankdetails.RemoveBankDetailsView

@Singleton
class RemoveBankDetailsController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val mcc: MessagesControllerComponents,
  view: RemoveBankDetailsView,
  implicit val error: views.html.ErrorView
) extends BankDetailsController(ds, mcc) {

  def get(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    getData[BankDetails](request.credId, index) map {
      case Some(BankDetails(_, Some(name), _, _, _, _, _)) =>
        Ok(view(index, name))
      case _                                               => NotFound(notFoundView)
    }
  }

  def remove(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    for {
      _ <- updateDataStrict[BankDetails](request.credId, index) { ba =>
             ba.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
           }
    } yield Redirect(routes.YourBankAccountsController.get())
  }
}
