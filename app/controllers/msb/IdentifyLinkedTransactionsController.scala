/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness => _}
import models.moneyservicebusiness._
import play.api.mvc.{MessagesControllerComponents, Result}
import utils.AuthAction

import views.html.msb.identify_linked_transactions

import scala.concurrent.Future

class IdentifyLinkedTransactionsController @Inject() (val dataCacheConnector: DataCacheConnector,
                                                      authAction: AuthAction,
                                                      val ds: CommonPlayDependencies,
                                                      val cc: MessagesControllerComponents,
                                                      identify_linked_transactions: identify_linked_transactions) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
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
        Redirect(routes.SummaryController.get)
    }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[IdentifyLinkedTransactions](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(identify_linked_transactions(f, edit)))
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll(request.credId) flatMap {
            optMap =>
              val result = for {
                cache <- optMap
                msb <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
              } yield {
                dataCacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
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
