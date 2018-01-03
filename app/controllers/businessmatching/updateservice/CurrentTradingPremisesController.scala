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

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.routes._
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.BusinessActivity
import models.businessmatching.updateservice._
import play.api.mvc.Result
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.current_trading_premises

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CurrentTradingPremisesController @Inject()(val authConnector: AuthConnector,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val businessMatchingService: BusinessMatchingService)() extends BaseController {

  private def failure(msg: String = "Unable to get business activities") = InternalServerError(msg)

  def get(index: Int = 0) = Authorised.async {
    implicit authContext => implicit request =>
      val result = getActivity(index) map { activity =>
        Ok(current_trading_premises(EmptyForm, activity, index))
      }

      result getOrElse failure()
  }

  def post(index: Int = 0) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[AreSubmittedActivitiesAtTradingPremises](request.body) match {
          case f: InvalidForm => getActivity(index) map { a => BadRequest(current_trading_premises(f, a, index)) } getOrElse failure()
          case ValidForm(_, data) =>
            (for {
              act <- activities
              redirect <- OptionT.liftF(redirectTo(data, index, act))
            } yield redirect) getOrElse failure()
        }
  }

  private def redirectTo(data: AreSubmittedActivitiesAtTradingPremises, index: Int, activities: Set[BusinessActivity])
                        (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    data match {
      case SubmittedActivitiesAtTradingPremisesYes if activitiesToIterate(index, activities) =>
        Future.successful(Redirect(CurrentTradingPremisesController.get(index + 1)))
      case SubmittedActivitiesAtTradingPremisesYes =>
        (businessMatchingService.fitAndProperRequired map {
          case true => Redirect(FitAndProperController.get())
          case false => Redirect(NewServiceInformationController.get())
        }) getOrElse InternalServerError("Cannot retrieve activities")
      case SubmittedActivitiesAtTradingPremisesNo =>
        Future.successful(Redirect(WhichCurrentTradingPremisesController.get(index)))
    }

  private def activities(implicit hc: HeaderCarrier, ac: AuthContext) = for {
    services <- businessMatchingService.getSubmittedBusinessActivities
  } yield services

  private def getActivity(index: Int)(implicit hc: HeaderCarrier, ac: AuthContext) = activities.map(_.toList(index))

  private def activitiesToIterate(index: Int, activities: Set[BusinessActivity]) =
    activities.size > index + 1

}