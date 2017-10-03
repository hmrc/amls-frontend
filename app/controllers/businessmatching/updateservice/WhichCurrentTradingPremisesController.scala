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

package controllers.businessmatching.updateservice

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.businessmatching.BusinessActivities
import models.tradingpremises.TradingPremises
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import views.html.businessmatching.updateservice.which_current_trading_premises

import scala.concurrent.ExecutionContext

class WhichCurrentTradingPremisesController @Inject()
  (val authConnector: AuthConnector, cacheConnector: DataCacheConnector, businessMatchingService: BusinessMatchingService) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      {
        for {
          tp <- getTradingPremises
          activities <- businessMatchingService.getSubmittedBusinessActivities
        } yield Ok(which_current_trading_premises(EmptyForm, tp, BusinessActivities.getValue(activities.head)))
      } getOrElse InternalServerError("Unable to get the trading premises")
  }

  def post() = Authorised.async {
    implicit authContext => implicit request => ???
  }

  private def getTradingPremises(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext) =
    OptionT(cacheConnector.fetch[Seq[TradingPremises]](TradingPremises.key))

}
