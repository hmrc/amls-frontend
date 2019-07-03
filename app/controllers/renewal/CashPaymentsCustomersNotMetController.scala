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

package controllers.renewal

import com.google.inject.Singleton
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.renewal.ReceiveCashPayments
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.cash_payments_customers_not_met

import scala.concurrent.Future

@Singleton
class CashPaymentsCustomersNotMetController @Inject()(
                                             val dataCacheConnector: DataCacheConnector,
                                             val authConnector: AuthConnector,
                                             val renewalService: RenewalService
                                           ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      renewalService.getRenewal map {
        response =>
          val form: Form2[ReceiveCashPayments] = (for {
            renewal <- response
            receivePayments <- renewal.receiveCashPayments
          } yield Form2[ReceiveCashPayments](receivePayments)).getOrElse(EmptyForm)
          Ok(cash_payments_customers_not_met(form, edit))
      } recoverWith {
        case _ => Future.successful(Ok(cash_payments_customers_not_met(EmptyForm, edit)))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ReceiveCashPayments](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(cash_payments_customers_not_met(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- renewalService.getRenewal
            _ <- renewalService.updateRenewal(renewal.receiveCashPayments(data))
          } yield Redirect(routes.HowCashPaymentsReceivedController.get())
      }
    }
  }

}