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

package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{CETransactionsInNext12Months, MoneyServiceBusiness}
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.msb.ce_transaction_in_next_12_months
import play.api.Play
import models.businessmatching.{MoneyServiceBusiness => MsbActivity}
import scala.concurrent.Future

trait CETransactionsInNext12MonthsController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  implicit val statusService: StatusService
  implicit val serviceFlow: ServiceFlow

  def get(edit:Boolean = false) = Authorised.async {
   implicit authContext => implicit request =>
     ControllerHelper.allowedToEdit(MsbActivity) flatMap {
       case true => dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
         response =>
           val form: Form2[CETransactionsInNext12Months] = (for {
             msb <- response
             transactions <- msb.ceTransactionsInNext12Months
           } yield Form2[CETransactionsInNext12Months](transactions)).getOrElse(EmptyForm)
           Ok(ce_transaction_in_next_12_months(form, edit))
       }
       case false => Future.successful(NotFound(notFoundView))
     }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CETransactionsInNext12Months](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(ce_transaction_in_next_12_months(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.ceTransactionsInNext12Months(data)
            )
          } yield edit match {
            case true if msb.whichCurrencies.isDefined =>
              Redirect(routes.SummaryController.get())
            case _ =>
              Redirect(routes.WhichCurrenciesController.get(edit))
          }
      }
    }
  }
}

object CETransactionsInNext12MonthsController extends CETransactionsInNext12MonthsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
  override lazy val serviceFlow = Play.current.injector.instanceOf[ServiceFlow]
}
