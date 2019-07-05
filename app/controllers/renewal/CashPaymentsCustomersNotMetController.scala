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
import models.renewal.{CashPayments, CashPaymentsCustomerNotMet, Renewal}
import services.RenewalService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
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
          val form: Form2[CashPaymentsCustomerNotMet] = (for {
            renewal <- response
            payments <- renewal.receiveCashPayments map { c => c.cashPaymentsCustomerNotMet }
          } yield Form2[CashPaymentsCustomerNotMet](payments)).getOrElse(EmptyForm)
          Ok(cash_payments_customers_not_met(form, edit))
      } recoverWith {
        case _ => Future.successful(Ok(cash_payments_customers_not_met(EmptyForm, edit)))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CashPaymentsCustomerNotMet](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(cash_payments_customers_not_met(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- renewalService.getRenewal
            _ <- updateCashPayments(data, renewal)
          } yield data match {
            case CashPaymentsCustomerNotMet(true) => Redirect(routes.HowCashPaymentsReceivedController.get(edit))
            case CashPaymentsCustomerNotMet(false) => Redirect(routes.SummaryController.get())
          }
      }
    }
  }

  private def updateCashPayments(data: CashPaymentsCustomerNotMet, renewal: Option[Renewal])
                                (implicit ac: AuthContext, hc: HeaderCarrier) = {

    val noCashPaymentFromCustomer = CashPayments(cashPaymentsCustomerNotMet = data, None)

    if (!data.receiveCashPayments) {
      renewalService.updateRenewal(renewal.receiveCashPayments(noCashPaymentFromCustomer))
    } else {
      renewalService.updateRenewal(renewal.receiveCashPayments(
        renewal.receiveCashPayments.map(
          rcp => CashPayments(data, rcp.howCashPaymentsReceived)).getOrElse(noCashPaymentFromCustomer)))
    }
  }
}