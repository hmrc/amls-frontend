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

package controllers.businessmatching.updateservice

import java.lang.ProcessBuilder.Redirect

import javax.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.updateservice._
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.status.{NotCompleted, SubmissionReady}
import play.api.mvc.{Request, Result}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import routes._

import scala.concurrent.Future
import cats.implicits._

@Singleton
class TradingPremisesController @Inject()(
                                           val authConnector: AuthConnector,
                                           val dataCacheConnector: DataCacheConnector,
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
          Form2[AreNewActivitiesAtTradingPremises](request.body) match {
            case ValidForm(_, data) => redirectTo(data, activities, index)
            case f: InvalidForm => Future.successful(
              BadRequest(views.html.businessmatching.updateservice.trading_premises(f, BusinessActivities.getValue(activity), index))
            )
          }
        }
  }

  private def redirectTo(data: AreNewActivitiesAtTradingPremises, additionalActivities: Set[BusinessActivity], index: Int)(implicit ac: AuthContext, hc: HeaderCarrier): Future[Result] = data match {
    case NewActivitiesAtTradingPremisesYes(_) => Future.successful(Redirect(WhichTradingPremisesController.get(index)))
    case NewActivitiesAtTradingPremisesNo =>
      if (activitiesToIterate(index, additionalActivities)) {
        Future.successful(Redirect(TradingPremisesController.get(index + 1)))
      } else {
        (businessMatchingService.fitAndProperRequired map {
          case true => Redirect(FitAndProperController.get())
          case false => Redirect(NewServiceInformationController.get())
        }) getOrElse InternalServerError("Cannot retrieve activities")
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
          case None =>
            Future.successful(InternalServerError("Cannot retrieve activities"))
        }
    } recoverWith {
      case _: IndexOutOfBoundsException | _: MatchError => Future.successful(NotFound(notFoundView))
    }
  }

}