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

import javax.inject.{Inject, Singleton}

import cats.implicits._
import controllers.BaseController
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.current_trading_premises
import forms.{EmptyForm, Form2, InvalidForm}
import models.businessmatching.updateservice.TradingPremisesSubmittedActivities
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CurrentTradingPremisesController @Inject()(val authConnector: AuthConnector,
                                                 val businessMatchingService: BusinessMatchingService)() extends BaseController {

  private def failure(msg: String = "Unable to get business activities") = InternalServerError(msg)

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      val result = getActivity map { activity =>
        Ok(current_trading_premises(EmptyForm, activity))
      }

      result getOrElse failure()
  }

  def post() = Authorised.async {
    implicit authContext => implicit request => {
        Form2[TradingPremisesSubmittedActivities](request.body) match {
          case f: InvalidForm => getActivity map { a => BadRequest(current_trading_premises(f, a)) } getOrElse failure()
        }
      }
  }

  private def getActivity(implicit hc: HeaderCarrier, ac: AuthContext) = for {
    services <- businessMatchingService.getSubmittedBusinessActivities
  } yield services.head

}