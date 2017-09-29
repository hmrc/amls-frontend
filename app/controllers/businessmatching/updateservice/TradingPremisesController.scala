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
import models.businessmatching.BusinessActivities
import models.status.{NotCompleted, SubmissionReady}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TradingPremisesController @Inject()(
                                           val authConnector: AuthConnector,
                                           val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService
                                         ) extends BaseController {

  def get(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          status <- OptionT.liftF(statusService.getStatus)
          additionalActivities <- businessMatchingService.getAdditionalBusinessActivities
        } yield {
          status match {
            case NotCompleted | SubmissionReady => NotFound(notFoundView)
            case _ => {
              val activity = additionalActivities.toList(index)
              Ok(views.html.businessmatching.updateservice.trading_premises(EmptyForm, BusinessActivities.getValue(activity), index))
            }
          }
        }) getOrElse InternalServerError("Cannot retrieve business activities")
  }

  def post(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request => {
        ???
      }
  }

}