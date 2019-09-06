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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import jto.validation.{Path, ValidationError}
import models.hvd.{Hvd, PaymentMethods}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import views.html.hvd.expect_to_receive

import scala.concurrent.Future

@Singleton
class ExpectToReceiveCashPaymentsController @Inject()( val authAction: AuthAction,
                                                       val ds: CommonPlayDependencies,
                                                       implicit val cacheConnector: DataCacheConnector,
                                                       implicit val statusService: StatusService,
                                                       implicit val serviceFlow: ServiceFlow) extends AmlsBaseController(ds) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      cacheConnector.fetch[Hvd](request.credId, Hvd.key) map {
        response =>
          val form: Form2[PaymentMethods] = (for {
            hvd <- response
            receivePayments <- hvd.cashPaymentMethods
          } yield {
            Form2[PaymentMethods](receivePayments)
          }).getOrElse(EmptyForm)

          Ok(expect_to_receive(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[PaymentMethods](request.body) match {
        case f: InvalidForm =>
          val message = "error.required.hvd.choose.option"
          findValidationMessage(f.errors, message) match {
            case true =>
              val paymentMethodsNotSelected = f.copy(errors = Seq((Path("paymentMethods"), Seq(ValidationError(Seq(message))))))
              Future.successful(BadRequest(expect_to_receive(paymentMethodsNotSelected, edit)))
            case _ =>
              Future.successful(BadRequest(expect_to_receive(f, edit)))
          }

        case ValidForm(_, data) =>
          for {
            hvd <- cacheConnector.fetch[Hvd](request.credId, Hvd.key)
            _ <- cacheConnector.save[Hvd](request.credId, Hvd.key,
              hvd.cashPaymentMethods(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
          }
      }
  }

  def findValidationMessage(formErrors: Seq[(Path, Seq[ValidationError])], message: String) =
    formErrors.flatMap(errors => errors._2.map(_.message == message)).contains(true)
}