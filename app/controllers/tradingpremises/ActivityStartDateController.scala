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

package controllers.tradingpremises

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises._
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

@Singleton
class ActivityStartDateController @Inject()(override val messagesApi: MessagesApi,
                                            val authConnector: AuthConnector,
                                            val dataCacheConnector: DataCacheConnector) extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>

        getData[TradingPremises](index) map {
          case Some(tp) => {
            val form = tp.yourTradingPremises match {
              case Some(YourTradingPremises(_,_,_,Some(date),_)) => Form2[ActivityStartDate](ActivityStartDate(date))
              case _ => EmptyForm
            }
            Ok(views.html.tradingpremises.activity_start_date(form, index, edit))
          }
          case None => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ActivityStartDate](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.tradingpremises.activity_start_date(f, index, edit)))
          case ValidForm(_, data) =>
            for {
              _ <- updateDataStrict[TradingPremises](index) { tp =>
                val ytp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None)(x => Some(x.copy(startDate = Some(data.startDate))))
                tp.copy(yourTradingPremises = ytp)
              }
            } yield edit match {
              case true => Redirect(routes.SummaryController.getIndividual(index))
              case false => Redirect(routes.IsResidentialController.get(index, edit))
            }
        }
  }
}
