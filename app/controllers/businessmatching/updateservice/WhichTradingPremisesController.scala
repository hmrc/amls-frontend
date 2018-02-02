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
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.businessmatching.updateservice.TradingPremisesActivities
import models.status.{NotCompleted, SubmissionReady}
import models.tradingpremises.TradingPremises
import services.{StatusService, TradingPremisesService}
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class WhichTradingPremisesController @Inject()(
                                                val authConnector: AuthConnector,
                                                val dataCacheConnector: DataCacheConnector,
                                                val statusService: StatusService,
                                                val businessMatchingService: BusinessMatchingService,
                                                val tradingPremisesService: TradingPremisesService
                                              )() extends BaseController with RepeatingSection {

  def get(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          status <- OptionT.liftF(statusService.getStatus)
          additionalActivities <- businessMatchingService.getAdditionalBusinessActivities
          tradingPremises <- OptionT.liftF(tradingPremises)
        } yield {
          try {
            status match {
              case st if !((st equals NotCompleted) | (st equals SubmissionReady)) =>
                val activity = additionalActivities.toList(index)
                Ok(views.html.businessmatching.updateservice.which_trading_premises(
                  EmptyForm,
                  tradingPremises,
                  BusinessActivities.getValue(activity),
                  index
                ))
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
          case st if !((st equals NotCompleted) | (st equals SubmissionReady)) =>
            businessMatchingService.getAdditionalBusinessActivities.value flatMap {
              case Some(additionalActivities) =>
                val activity = additionalActivities.toList(index)
                Form2[TradingPremisesActivities](request.body) match {
                  case ValidForm(_, data) =>
                    redirectTo(index, additionalActivities, activity, data)
                  case f: InvalidForm =>
                    tradingPremises map { tp =>
                      BadRequest(views.html.businessmatching.updateservice.which_trading_premises(
                        f,
                        tp,
                        BusinessActivities.getValue(activity),
                        index
                      ))
                    }
                }
              case None => Future.successful(InternalServerError("Cannot retrieve activities"))
            }
        }
      } recoverWith {
        case _: IndexOutOfBoundsException | _: MatchError => Future.successful(NotFound(notFoundView))
      }
  }

  private def redirectTo(index: Int, additionalActivities: Set[BusinessActivity], activity: BusinessActivity, data: TradingPremisesActivities)
                        (implicit ac: AuthContext, hc: HeaderCarrier) = {
    updateTradingPremises(data, activity) flatMap { _ =>
      if (businessMatchingService.activitiesToIterate(index, additionalActivities)) {
        Future.successful(Redirect(TradingPremisesController.get(index + 1)))
      } else {
        businessMatchingService.fitAndProperRequired.value map {
          case Some(true) => Redirect(FitAndProperController.get())
          case Some(false) => Redirect(NewServiceInformationController.get())
          case _ => InternalServerError("Cannot retrieve activities")
        }
      }
    }
  }

  private def updateTradingPremises(data: TradingPremisesActivities, activity: BusinessActivity)
                                   (implicit ac: AuthContext, hc: HeaderCarrier): Future[_] =
    updateDataStrict[TradingPremises] { tradingPremises: Seq[TradingPremises] =>
      tradingPremisesService.addBusinessActivtiesToTradingPremises(data.index.toSeq, tradingPremises, activity, false)
    }

  private def tradingPremises(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[(TradingPremises, Int)]] =
    getData[TradingPremises].map { tradingpremises =>
      tradingpremises.zipWithIndex.filterNot { case (tp, _) =>
        tp.status.contains(StatusConstants.Deleted) | !tp.isComplete
      }
    }

}