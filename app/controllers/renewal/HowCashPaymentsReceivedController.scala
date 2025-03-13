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
import forms.renewal.HowCashPaymentsReceivedFormProvider
import models.renewal.{CashPayments, CashPaymentsCustomerNotMet}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.HowCashPaymentsReceivedView

import javax.inject.Inject
import scala.concurrent.Future

@Singleton
class HowCashPaymentsReceivedController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val cc: MessagesControllerComponents,
  formProvider: HowCashPaymentsReceivedFormProvider,
  view: HowCashPaymentsReceivedView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    renewalService.getRenewal(request.credId) map { response =>
      val form = (for {
        renewal  <- response
        payments <- renewal.receiveCashPayments.flatMap(_.howCashPaymentsReceived)
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
            _       <- renewalService.updateRenewal(
                         request.credId,
                         renewal.receiveCashPayments(renewal.receiveCashPayments match {
                           case Some(cp) if cp.cashPaymentsCustomerNotMet.receiveCashPayments  =>
                             CashPayments(CashPaymentsCustomerNotMet(true), Some(data))
                           case Some(cp) if !cp.cashPaymentsCustomerNotMet.receiveCashPayments =>
                             CashPayments(CashPaymentsCustomerNotMet(false), None)
                           case _                                                              => CashPayments(CashPaymentsCustomerNotMet(false), None)
                         })
                       )
          } yield Redirect(routes.SummaryController.get)
      )
  }

}
