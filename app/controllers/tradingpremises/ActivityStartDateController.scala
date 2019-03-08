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

package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import javax.inject.{Inject, Singleton}
import models.tradingpremises._
import play.api.i18n.MessagesApi
import play.api.mvc.Result
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
          case Some(tpSection) =>
            tpSection.yourTradingPremises match {
              case Some(YourTradingPremises(_, tradingPremisesAddress, _, None, _)) =>
                Ok(views.html.tradingpremises.activity_start_date(EmptyForm, index, edit, tradingPremisesAddress))
              case Some(YourTradingPremises(_, tradingPremisesAddress, _, Some(startDate), _)) =>
                Ok(views.html.tradingpremises.activity_start_date(Form2[ActivityStartDate](ActivityStartDate(startDate)), index, edit, tradingPremisesAddress))
              case _ =>
               NotFound(notFoundView)
            }
          case _ =>
            NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ActivityStartDate](request.body) match {
          case f: InvalidForm =>
            for {
              tp <- getData[TradingPremises](index)
            } yield tp.flatMap(_.yourTradingPremises) match {
              case Some(ytp) =>
                    BadRequest(views.html.tradingpremises.activity_start_date (f, index, edit, ytp.tradingPremisesAddress) )
              case _ => NotFound(notFoundView)
            }
          case ValidForm(_, data) =>
            for {
              _ <- updateDataStrict[TradingPremises](index) { tp =>
                val ytp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None)(x => Some(x.copy(startDate = Some(data.startDate))))
                tp.copy(yourTradingPremises = ytp)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index))
              case false => Redirect(routes.IsResidentialController.get(index, edit))
            }
        }
  }
}
