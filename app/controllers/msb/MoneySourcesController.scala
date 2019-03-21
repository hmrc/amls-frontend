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

package controllers.msb

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.{BusinessMatching, CurrencyExchange, ForeignExchange, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness._
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future

class MoneySourcesController @Inject()(val authConnector: AuthConnector,
                                       implicit val dataCacheConnector: DataCacheConnector,
                                       implicit val statusService: StatusService,
                                       implicit val serviceFlow: ServiceFlow
                                          ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        ControllerHelper.allowedToEdit(MsbActivity, Some(CurrencyExchange)) flatMap {
          case true => dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
            response =>
              val form: Form2[MoneySources] = (for {
                msb <- response
                currencies <- msb.whichCurrencies
                moneySources <- currencies.moneySources
              } yield Form2[MoneySources](moneySources)).getOrElse(EmptyForm)

              Ok(views.html.msb.money_sources(form, edit))
          }
          case false => Future.successful(NotFound(notFoundView))
        }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        val foo = Form2[MoneySources](request.body)
        foo match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.msb.money_sources(f, edit)))
          case ValidForm(_, data: MoneySources) =>
            for {
              msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
              cache <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
                updateMoneySources(msb, data))
            } yield redirectToNextPage(cache, edit).getOrElse(NotFound(notFoundView))


        }
      }
  }

  def redirectToNextPage(map: CacheMap, edit: Boolean) = {
    for {
      msb <- map.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
      bm <- map.getEntry[BusinessMatching](BusinessMatching.key)
      services <- bm.msbServices
    } yield {
      services.msbServices.contains(ForeignExchange) match {
        case true if msb.fxTransactionsInNext12Months.isEmpty || !edit =>
          Redirect(routes.FXTransactionsInNext12MonthsController.get(edit))
        case _ => Redirect(routes.SummaryController.get())
      }
    }
  }

  def updateMoneySources(oldMsb: Option[MoneyServiceBusiness], moneySources: MoneySources): Option[MoneyServiceBusiness] = {
    oldMsb match {
      case Some(msb) => {
        msb.whichCurrencies match {
          case Some(w) => Some(msb.whichCurrencies(w.moneySources(moneySources)))
          case _ => None
        }
      }
      case _ => None
    }
  }
}
