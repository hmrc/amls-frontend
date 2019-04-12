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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.renewal._
import services.RenewalService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.uses_foreign_currencies

import scala.concurrent.Future

class UsesForeignCurrenciesController @Inject()(val authConnector: AuthConnector,
                                                renewalService: RenewalService,
                                                dataCacheConnector: DataCacheConnector) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        val block = for {
          renewal <- OptionT(renewalService.getRenewal)
          whichCurrencies <- OptionT.fromOption[Future](renewal.whichCurrencies)
          ufc <- OptionT.fromOption[Future](whichCurrencies.usesForeignCurrencies)
        } yield {
          Ok(uses_foreign_currencies(Form2[UsesForeignCurrencies](ufc), edit))
        }

        block getOrElse Ok(uses_foreign_currencies(EmptyForm, edit))

  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[UsesForeignCurrencies](request.body) match {
          case f: InvalidForm => Future.successful(BadRequest(uses_foreign_currencies(f, edit)))
          case ValidForm(_, model) =>
            dataCacheConnector.fetchAll flatMap {
              optMap =>
                val result = for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  services <- bm.msbServices
                } yield {
                  renewalService.updateRenewal(updateCurrencies(renewal, model)) map { _ =>
                    routing(services.msbServices, edit, model)
                  }
                }
                result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  def updateCurrencies(oldRenewal: Renewal, usesForeignCurrencies: UsesForeignCurrencies) = {
    oldRenewal.whichCurrencies match {
      case Some(wc) if usesForeignCurrencies.equals(UsesForeignCurrenciesYes) => {
        val newWc = wc.usesForeignCurrencies(usesForeignCurrencies)
        oldRenewal.whichCurrencies(newWc)
      }
      case Some(wc) if usesForeignCurrencies.equals(UsesForeignCurrenciesNo) => {
        val newWc = wc.usesForeignCurrencies(usesForeignCurrencies).moneySources(MoneySources())
        oldRenewal.whichCurrencies(newWc)
      }
      case _ => oldRenewal
    }
  }

  def routing(msbServices: Set[BusinessMatchingMsbService],
              edit: Boolean,
              data: UsesForeignCurrencies)(implicit hc: HeaderCarrier, auth: AuthContext) = {
    if (data == UsesForeignCurrenciesYes) {
      Redirect(routes.MoneySourcesController.get(edit))
    } else if (msbServices.contains(ForeignExchange) && !edit) {
      Redirect(routes.FXTransactionsInLast12MonthsController.get(edit))
    } else {
      Redirect(routes.SummaryController.get())
    }
  }
}