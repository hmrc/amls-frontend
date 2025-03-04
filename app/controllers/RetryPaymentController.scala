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

package controllers

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import javax.inject.{Inject, Singleton}
import play.api.mvc.MessagesControllerComponents
import services.{PaymentsService, StatusService}
import utils.AuthAction

import scala.concurrent.Future

@Singleton
class RetryPaymentController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  private[controllers] implicit val dataCacheConnector: DataCacheConnector,
  private[controllers] implicit val amlsConnector: AmlsConnector,
  private[controllers] implicit val statusService: StatusService,
  private[controllers] val paymentsService: PaymentsService,
  val cc: MessagesControllerComponents
) extends AmlsBaseController(ds, cc) {

  def retryPayment = authAction.async { implicit request =>
    val result = for {
      form       <- OptionT.fromOption[Future](request.body.asFormUrlEncoded)
      paymentRef <- OptionT.fromOption[Future](form("paymentRef").headOption)
      oldPayment <- OptionT(amlsConnector.getPaymentByPaymentReference(paymentRef, request.accountTypeId))
      nextUrl    <- OptionT.liftF(
                      paymentsService.paymentsUrlOrDefault(
                        paymentRef,
                        oldPayment.amountInPence.toDouble / 100,
                        controllers.routes.PaymentConfirmationController.paymentConfirmation(paymentRef).url,
                        oldPayment.amlsRefNo,
                        oldPayment.safeId,
                        request.accountTypeId
                      )
                    )
    } yield Redirect(nextUrl.value)

    result getOrElse InternalServerError("Unable to retry payment due to a failure")
  }
}
