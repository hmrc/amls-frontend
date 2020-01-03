/*
 * Copyright 2020 HM Revenue & Customs
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
import models.businessmatching.{BusinessMatching, ForeignExchange}
import models.moneyservicebusiness._
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class MoneySourcesController @Inject()(authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       implicit val dataCacheConnector: DataCacheConnector,
                                       implicit val statusService: StatusService,
                                       implicit val serviceFlow: ServiceFlow,
                                       val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request => {
        dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
          response =>
            val form: Form2[MoneySources] = (for {
              msb <- response
              currencies <- msb.whichCurrencies
              moneySources <- currencies.moneySources
            } yield Form2[MoneySources](moneySources)).getOrElse(EmptyForm)

            Ok(views.html.msb.money_sources(form, edit))
        }
      }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request => {
        Form2[MoneySources](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.msb.money_sources(f, edit)))
          case ValidForm(_, data: MoneySources) =>
            for {
              msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
              _ <- dataCacheConnector.save[MoneyServiceBusiness](request.credId,MoneyServiceBusiness.key, updateMoneySources(msb, data))
              updatedMsb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
              bm <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
            } yield redirectToNextPage(updatedMsb, bm, edit).getOrElse(NotFound(notFoundView))
        }
      }
  }

  def redirectToNextPage(maybeMsb: Option[MoneyServiceBusiness], maybeBm: Option[BusinessMatching], edit: Boolean) = {
    for {
      msb <- maybeMsb
      bm <- maybeBm
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
