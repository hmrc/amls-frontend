/*
 * Copyright 2018 HM Revenue & Customs
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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.EmptyForm
import javax.inject.Inject
import models.bankdetails.BankDetails
import models.bankdetails.BankDetails.Filters._
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class YourBankAccountsController @Inject()(
                                            val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector = AMLSAuthConnector,
                                            val statusService: StatusService
                                          ) extends BankDetailsController {
  def get(complete: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          bankDetails <- dataCacheConnector.fetch[Seq[BankDetails]](BankDetails.key)
        } yield bankDetails match {
          case Some(data) =>
            val filteredBankDetails = data.zipWithIndex.visibleAccounts

            Ok(views.html.bankdetails.your_bank_accounts(
              EmptyForm,
              filteredBankDetails.incompleteAccounts,
              filteredBankDetails.completeAccounts
            ))

          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }
}