/*
 * Copyright 2017 HM Revenue & Customs
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
import controllers.BaseController
import forms.EmptyForm
import models.bankdetails.BankDetails
import utils.{StatusConstants, RepeatingSection}

trait RemoveBankDetailsController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[BankDetails](index) map {
        case Some(BankDetails(_, Some(bankAcct), _,_,_)) =>
        Ok(views.html.bankdetails.remove_bank_details(EmptyForm, index, bankAcct.accountName, complete))
        case _ => NotFound(notFoundView)
      }
  }

  def remove(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      for {
        rs <- updateDataStrict[BankDetails](index) { ba =>
          ba.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
        }
      } yield Redirect(routes.SummaryController.get(complete))
    }
  }
}

object RemoveBankDetailsController extends RemoveBankDetailsController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
