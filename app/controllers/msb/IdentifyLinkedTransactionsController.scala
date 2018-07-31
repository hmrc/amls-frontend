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

package controllers.msb

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.{MoneyServiceBusiness => _, _}
import models.moneyservicebusiness._
import play.api.mvc.Result
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.identify_linked_transactions

import scala.concurrent.Future

class IdentifyLinkedTransactionsController @Inject() (val dataCacheConnector: DataCacheConnector,
                                                      val authConnector: AuthConnector
                                                     ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[IdentifyLinkedTransactions] = (for {
            msb <- response
            transactions <- msb.identifyLinkedTransactions
          } yield Form2[IdentifyLinkedTransactions](transactions)).getOrElse(EmptyForm)
          Ok(identify_linked_transactions(form, edit))
      }
  }

  private def routing(services: Set[BusinessMatchingMsbService], msb: MoneyServiceBusiness, edit: Boolean): Result =
    if (services.contains(TransmittingMoney) && (msb.businessUseAnIPSP.isEmpty || !edit)) {
        Redirect(routes.BusinessUseAnIPSPController.get(edit))
    } else if (services.contains(CurrencyExchange) && (msb.ceTransactionsInNext12Months.isEmpty || !edit)) {
        Redirect(routes.CETransactionsInNext12MonthsController.get(edit))
    } else if (services.contains(ForeignExchange) && (msb.fxTransactionsInNext12Months.isEmpty || !edit)) {
        Redirect(routes.FXTransactionsInNext12MonthsController.get(edit))
    } else {
        Redirect(routes.SummaryController.get())
    }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[IdentifyLinkedTransactions](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(identify_linked_transactions(f, edit)))
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll flatMap {
            optMap =>
              val result = for {
                cache <- optMap
                msb <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
              } yield {
                dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
                  msb.identifyLinkedTransactions(data)
                ) map {
                  _ => routing(services.msbServices, msb, edit)
                }
              }
              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      }
    }
  }
}
