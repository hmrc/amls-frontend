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

import cats.data.OptionT
import cats.implicits._
import controllers.BaseController
import forms.EmptyForm
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class TradingPremisesController @Inject()(
                                           val authConnector: AuthConnector,
                                           val businessMatchingService: BusinessMatchingService
                                         ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          businessMatching <- businessMatchingService.getModel
          businessActivities <- OptionT.fromOption[Future](businessMatching.activities)
        } yield {
          Ok(views.html.businessmatching.updateservice.trading_premises(EmptyForm))
        }) getOrElse InternalServerError("Cannot retrieve business activities")
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request => {
        ???
      }
  }

}