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

import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.updateservice.{TradingPremisesNewActivities, TradingPremisesNewActivitiesNo, TradingPremisesNewActivitiesYes}
import models.businessmatching.{BusinessActivities, BusinessActivity}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future

@Singleton
class TradingPremisesController @Inject()(
                                           val authConnector: AuthConnector,
                                           implicit val statusService: StatusService,
                                           implicit val businessMatchingService: BusinessMatchingService
                                         ) extends BaseController {

  def get(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request =>
        ControllerHelper.additionalActivityForTradingPremises(index){ activity: BusinessActivity =>
          Future.successful(Ok(views.html.businessmatching.updateservice.trading_premises(EmptyForm, BusinessActivities.getValue(activity), index)))
        }
  }

  def post(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request =>
        ControllerHelper.additionalActivityForTradingPremises(index){ activity: BusinessActivity =>
          Form2[TradingPremisesNewActivities](request.body) match {
            case ValidForm(_, data) => Future.successful(redirectTo(data, index))
            case f: InvalidForm => Future.successful(
              BadRequest(views.html.businessmatching.updateservice.trading_premises(f, BusinessActivities.getValue(activity), index))
            )
          }
        }
  }

  private def redirectTo(data: TradingPremisesNewActivities, index: Int) = data match {
    case TradingPremisesNewActivitiesYes(_) => Redirect(routes.WhichTradingPremisesController.get(index))
    case TradingPremisesNewActivitiesNo => Redirect(routes.CurrentTradingPremisesController.get())
  }

}