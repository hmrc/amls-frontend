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
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.updateservice._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import views.html.businessmatching.updateservice.current_trading_premises

@Singleton
class CurrentTradingPremisesController @Inject()(val authConnector: AuthConnector,
                                                 val dataCacheConnector: DataCacheConnector,
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
        Form2[AreSubmittedActivitiesAtTradingPremises](request.body) match {
          case f: InvalidForm => getActivity map { a => BadRequest(current_trading_premises(f, a)) } getOrElse failure()
          case ValidForm(_, data) => {
            (for {
              updateService <- OptionT(dataCacheConnector.fetch[UpdateService](UpdateService.key))
              _ <- OptionT.liftF(dataCacheConnector.save[UpdateService](UpdateService.key, updateService.copy(
                 areSubmittedActivitiesAtTradingPremises = Some(data)
              )))
            } yield {
              data match {
                case SubmittedActivitiesAtTradingPremisesYes =>
                  Redirect(controllers.routes.RegistrationProgressController.get())
                case SubmittedActivitiesAtTradingPremisesNo =>
                  Redirect(controllers.businessmatching.updateservice.routes.WhichCurrentTradingPremisesController.get())
              }
            }) getOrElse InternalServerError("Could not update service")
          }
        }
      }
  }

  private def getActivity(implicit hc: HeaderCarrier, ac: AuthContext) = for {
    services <- businessMatchingService.getSubmittedBusinessActivities
  } yield services.head

}