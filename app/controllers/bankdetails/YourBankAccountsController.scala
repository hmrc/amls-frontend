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
import javax.inject.Inject
import models.bankdetails.BankDetails
import models.bankdetails.BankDetails.Filters._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.bankdetails.YourBankAccountsView

class YourBankAccountsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val mcc: MessagesControllerComponents,
  view: YourBankAccountsView
) extends BankDetailsController(ds, mcc) {

  def get(complete: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    for {
      bankDetails <- dataCacheConnector.fetch[Seq[BankDetails]](request.credId, BankDetails.key)
    } yield bankDetails match {
      case Some(data) =>
        val filteredBankDetails = data.zipWithIndex.visibleAccounts.reverse
        val result              = Ok(
          view(
            filteredBankDetails.incompleteAccounts,
            filteredBankDetails.completeAccounts
          )
        )

        if (filteredBankDetails.incompleteAccounts.isEmpty) {
          result.removingFromSession("itemIndex")
        } else {
          result
        }

      case _ => Redirect(controllers.routes.RegistrationProgressController.get())
    }
  }
}
