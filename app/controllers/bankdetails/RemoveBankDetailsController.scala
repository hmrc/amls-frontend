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

package controllers.bankdetails

import connectors.DataCacheConnector
import controllers.CommonPlayDependencies
import forms.EmptyForm
import javax.inject.{Inject, Singleton}
import models.bankdetails.BankDetails
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, StatusConstants}
import views.html.bankdetails.remove_bank_details

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RemoveBankDetailsController @Inject()(val authAction: AuthAction,
                                            val ds: CommonPlayDependencies,
                                            val dataCacheConnector: DataCacheConnector,
                                            val mcc: MessagesControllerComponents,
                                            remove_bank_details: remove_bank_details,
                                            implicit val error: views.html.error) extends BankDetailsController(ds, mcc) {

  def get(index: Int) = authAction.async {
      implicit request =>
        getData[BankDetails](request.credId, index) map {
          case Some(BankDetails(_, Some(name), _, _, _, _, _)) =>
            Ok(remove_bank_details(EmptyForm, index, name))
          case _ => NotFound(notFoundView)
        }
  }

  def remove(index: Int) = authAction.async {
      implicit request => {
        for {
          _ <- updateDataStrict[BankDetails](request.credId, index) { ba =>
            ba.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
          }
        } yield Redirect(routes.YourBankAccountsController.get())
      }
  }
}