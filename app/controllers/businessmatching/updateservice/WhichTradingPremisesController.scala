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
import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.businessmatching.updateservice.{TradingPremises => BMTradingPremises}
import models.status.{NotCompleted, SubmissionReady}
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import play.api.Logger
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.RepeatingSection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class WhichTradingPremisesController @Inject()(
                                                val authConnector: AuthConnector,
                                                val dataCacheConnector: DataCacheConnector,
                                                val statusService: StatusService,
                                                val businessMatchingService: BusinessMatchingService
                                              )() extends BaseController with RepeatingSection {


  def get(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          status <- OptionT.liftF(statusService.getStatus)
          additionalActivities <- businessMatchingService.getAdditionalBusinessActivities
          tradingPremises <- OptionT.liftF(getData[TradingPremises])
        } yield {
          try {
            status match {
              case st if !((st equals NotCompleted) | (st equals SubmissionReady)) => {
                val activity = additionalActivities.toList(index)
                Ok(views.html.businessmatching.updateservice.which_trading_premises(
                  EmptyForm,
                  tradingPremises,
                  BusinessActivities.getValue(activity),
                  index
                ))
              }
            }
          } catch {
            case _: IndexOutOfBoundsException | _: MatchError => NotFound(notFoundView)
          }
        }) getOrElse InternalServerError("Cannot retrieve business activities")
  }

  def post(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request => {
        statusService.getStatus flatMap {
          case st if !((st equals NotCompleted) | (st equals SubmissionReady)) => {
            businessMatchingService.getAdditionalBusinessActivities.value flatMap {
              case Some(additionalActivities) => {
                val activity = additionalActivities.toList(index)
                Form2[BMTradingPremises](request.body) match {
                  case ValidForm(_, data) => {
                    updateTradingPremises(data, activity) map { _ =>
                      if (activitiesToIterate(index, additionalActivities)) {
                        Redirect(routes.TradingPremisesController.get(index + 1))
                      } else {
                        Redirect(routes.CurrentTradingPremisesController.get())
                      }
                    }
                  }
                  case f: InvalidForm =>
                    getData[TradingPremises] map { tradingPremises =>
                      BadRequest(views.html.businessmatching.updateservice.which_trading_premises(
                        f,
                        tradingPremises,
                        BusinessActivities.getValue(activity),
                        index
                      ))
                    }
                }
              }
              case None => Future.successful(InternalServerError("Cannot retrieve activities"))
            }
          }
        }
      } recoverWith {
        case _: IndexOutOfBoundsException | _: MatchError => Future.successful(NotFound(notFoundView))
      }
  }

  private def activitiesToIterate(index: Int, additionalActivities: Set[BusinessActivity]) =
    additionalActivities.size > index + 1

  private def updateTradingPremises(data: BMTradingPremises, activity: BusinessActivity)(implicit ac: AuthContext, hc: HeaderCarrier): Future[_] = {
    println(">>>>" + data)
    updateDataStrict[TradingPremises](data.index.head) { tradingPremises =>
      tradingPremises.whatDoesYourBusinessDoAtThisAddress(
        tradingPremises.whatDoesYourBusinessDoAtThisAddress.fold(WhatDoesYourBusinessDo(Set(activity))) { wdybd =>
          wdybd.copy(
            wdybd.activities + activity
          )
        }
      ).copy(hasAccepted = true)
    }
  }

}