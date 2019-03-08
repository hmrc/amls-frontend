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

package controllers.bankdetails

import connectors.DataCacheConnector
import forms.EmptyForm
import javax.inject.{Inject, Singleton}
import models.bankdetails.BankDetails
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.StatusConstants

@Singleton
class RemoveBankDetailsController @Inject()(
                                             val authConnector: AuthConnector,
                                             val dataCacheConnector: DataCacheConnector
                                           ) extends BankDetailsController {

  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[BankDetails](index) map {
          case Some(BankDetails(_, Some(name), _, _, _, _, _)) =>
            Ok(views.html.bankdetails.remove_bank_details(EmptyForm, index, name))
          case _ => NotFound(notFoundView)
        }
  }

  def remove(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request => {
        for {
          _ <- updateDataStrict[BankDetails](index) { ba =>
            ba.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
          }
        } yield Redirect(routes.YourBankAccountsController.get())
      }
  }
}