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

  private def standardRouting(services: Set[BusinessMatchingMsbService]): Result =
    services match {
      case s if s contains TransmittingMoney =>
        Redirect(routes.BusinessUseAnIPSPController.get())
      case s if s contains CurrencyExchange =>
        Redirect(routes.CETransactionsInNext12MonthsController.get())
      case s if s contains ForeignExchange =>
        Redirect(routes.FXTransactionsInNext12MonthsController.get())
      case _ =>
        Redirect(routes.SummaryController.get())
    }

  private def editRouting(services: Set[BusinessMatchingMsbService], msb: MoneyServiceBusiness): Result =
    services match {
      case s if s contains TransmittingMoney =>
        mtRouting(services, msb)
      case s if s contains CurrencyExchange =>
        ceRouting(msb)
      case s if s contains ForeignExchange =>
        fxRouting(msb)
      case _ =>
        Redirect(routes.SummaryController.get())
    }

  private def mtRouting(services: Set[BusinessMatchingMsbService], msb: MoneyServiceBusiness): Result =
    if (msb.businessUseAnIPSP.isDefined) {
      editRouting(services - TransmittingMoney, msb)
    } else {
      Redirect(routes.BusinessUseAnIPSPController.get(true))
    }

  private def ceRouting(msb: MoneyServiceBusiness): Result =
    if (msb.ceTransactionsInNext12Months.isDefined) {
      Redirect(routes.SummaryController.get())
    } else {
      Redirect(routes.CETransactionsInNext12MonthsController.get(true))
    }

  private def fxRouting(msb: MoneyServiceBusiness): Result =
    if (msb.fxTransactionsInNext12Months.isDefined) {
      Redirect(routes.SummaryController.get())
    } else {
      Redirect(routes.FXTransactionsInNext12MonthsController.get(true))
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
                  _ =>
                    if (edit) {
                      editRouting(services.msbServices, msb)
                    } else {
                      standardRouting(services.msbServices)
                    }
                }
              }
              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      }
    }
  }
}
