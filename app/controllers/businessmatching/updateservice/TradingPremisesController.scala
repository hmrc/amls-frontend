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
import models.status.{NotCompleted, SubmissionReady}
import play.api.mvc.{Request, Result}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class TradingPremisesController @Inject()(
                                           val authConnector: AuthConnector,
                                           val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService
                                         ) extends BaseController {

  def get(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request =>
        additionalActivityForTradingPremises(index){ (_, activity: BusinessActivity) =>
          Future.successful(Ok(views.html.businessmatching.updateservice.trading_premises(EmptyForm, BusinessActivities.getValue(activity), index)))
        }
  }

  def post(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request =>
        additionalActivityForTradingPremises(index){ (activities: Set[BusinessActivity], activity: BusinessActivity) =>
          Form2[TradingPremisesNewActivities](request.body) match {
            case ValidForm(_, data) => Future.successful(redirectTo(data, activities, index))
            case f: InvalidForm => Future.successful(
              BadRequest(views.html.businessmatching.updateservice.trading_premises(f, BusinessActivities.getValue(activity), index))
            )
          }
        }
  }

  private def redirectTo(data: TradingPremisesNewActivities, additionalActivities: Set[BusinessActivity], index: Int) = data match {
    case TradingPremisesNewActivitiesYes(_) => Redirect(routes.WhichTradingPremisesController.get(index))
    case TradingPremisesNewActivitiesNo => {
      if (activitiesToIterate(index, additionalActivities)) {
        Redirect(routes.TradingPremisesController.get(index + 1))
      } else {
        Redirect(routes.CurrentTradingPremisesController.get())
      }
    }
  }

  private def activitiesToIterate(index: Int, additionalActivities: Set[BusinessActivity]) =
    additionalActivities.size > index + 1

  def additionalActivityForTradingPremises(index: Int)
                                          (fn: ((Set[BusinessActivity], BusinessActivity) => Future[Result]))
                                          (implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]) = {
    statusService.getStatus flatMap {
      case st if !((st equals NotCompleted) | (st equals SubmissionReady)) =>
        businessMatchingService.getAdditionalBusinessActivities.value flatMap {
          case Some(additionalActivities) =>
            val activity = additionalActivities.toList(index)
            fn(additionalActivities, activity)
          case None => Future.successful(InternalServerError("Cannot retrieve activities"))
        }
    } recoverWith {
      case _: IndexOutOfBoundsException | _: MatchError => Future.successful(NotFound(notFoundView))
    }
  }

}