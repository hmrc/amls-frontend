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

package controllers.renewal

import com.google.inject.Singleton
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.CashPaymentsCustomersNotMetFormProvider
import models.renewal.{CashPayments, CashPaymentsCustomerNotMet, Renewal}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import services.cache.Cache
import utils.AuthAction
import views.html.renewal.CashPaymentsCustomersNotMetView

import javax.inject.Inject
import scala.concurrent.Future

@Singleton
class CashPaymentsCustomersNotMetController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val cc: MessagesControllerComponents,
  formProvider: CashPaymentsCustomersNotMetFormProvider,
  view: CashPaymentsCustomersNotMetView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    renewalService.getRenewal(request.credId).map { response =>
      val form = (for {
        renewal  <- response
        payments <- renewal.receiveCashPayments map { c => c.cashPaymentsCustomerNotMet }
      } yield formProvider().fill(payments)).getOrElse(formProvider())
      Ok(view(form, edit))
    } recoverWith { case _ =>
      Future.successful(Ok(view(formProvider(), edit)))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            renewal <- renewalService.getRenewal(request.credId)
            _       <- updateCashPayments(request.credId, data, renewal)
          } yield data match {
            case CashPaymentsCustomerNotMet(true)  => Redirect(routes.HowCashPaymentsReceivedController.get(edit))
            case CashPaymentsCustomerNotMet(false) => Redirect(routes.SummaryController.get)
          }
      )
  }

  private def updateCashPayments(
    credId: String,
    data: CashPaymentsCustomerNotMet,
    renewal: Option[Renewal]
  ): Future[Cache] = {

    val noCashPaymentFromCustomer = CashPayments(cashPaymentsCustomerNotMet = data, None)

    if (!data.receiveCashPayments) {
      renewalService.updateRenewal(credId, renewal.receiveCashPayments(noCashPaymentFromCustomer))
    } else {
      renewalService.updateRenewal(
        credId,
        renewal.receiveCashPayments(
          renewal.receiveCashPayments
            .map(rcp => CashPayments(data, rcp.howCashPaymentsReceived))
            .getOrElse(noCashPaymentFromCustomer)
        )
      )
    }
  }
}
