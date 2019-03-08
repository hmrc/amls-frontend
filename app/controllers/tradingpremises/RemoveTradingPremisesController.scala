/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.tradingpremises

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.status._
import models.tradingpremises.{ActivityEndDate, TradingPremises}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, StatusConstants}
import views.html.tradingpremises.remove_trading_premises

import scala.concurrent.Future

class RemoveTradingPremisesController @Inject () (
                                                   val dataCacheConnector: DataCacheConnector,
                                                   val authConnector: AuthConnector,
                                                   val statusService: StatusService
                                                 ) extends RepeatingSection with BaseController {

  def get(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        tp <- getData[TradingPremises](index)
        status <- statusService.getStatus
      } yield (tp, status) match {

        case (Some(_), SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_)) =>
          Ok (
            views.html.tradingpremises.remove_trading_premises (
              f = EmptyForm,
              index = index,
              complete = complete,
              tradingAddress = tp.yourTradingPremises.fold("")(_.tradingPremisesAddress.toLines.mkString(", ")),
              showDateField = tp.lineId.isDefined
            )
          )

        case (Some(_), _) => Ok (
          views.html.tradingpremises.remove_trading_premises (
            f = EmptyForm,
            index = index,
            complete = complete,
            tradingAddress = tp.yourTradingPremises.fold("")(_.tradingPremisesAddress.toLines.mkString(",")),
            showDateField = false
          )
        )

        case _ => NotFound(notFoundView)
      }
  }

  def remove(index: Int, complete: Boolean = false, tradingAddress: String) = Authorised.async {
    implicit authContext => implicit request =>

      def removeWithoutDate = removeDataStrict[TradingPremises](index) map { _ =>
        Redirect(routes.YourTradingPremisesController.get())
      }

      statusService.getStatus flatMap {
        case NotCompleted | SubmissionReady => removeDataStrict[TradingPremises](index) map { _ =>
          Redirect(routes.YourTradingPremisesController.get(complete))
        }
        case SubmissionReadyForReview => for {
          _ <- updateDataStrict[TradingPremises](index) { tp =>
            tp.copy(status = Some(StatusConstants.Deleted), hasChanged = true)

          }
        } yield Redirect(routes.YourTradingPremisesController.get(complete))
        case _ =>
          getData[TradingPremises](index) flatMap { premises =>
            premises.lineId match {
              case Some(tp) =>
                val extraFields = Map(
                  "premisesStartDate" -> Seq(premises.get.yourTradingPremises.get.startDate.get.toString("yyyy-MM-dd"))
                )

                Form2[ActivityEndDate](request.body.asFormUrlEncoded.get ++ extraFields) match {
                  case f: InvalidForm =>
                    Future.successful(
                      BadRequest(
                        remove_trading_premises(
                          f = f,
                          index = index,
                          complete = complete,
                          tradingAddress = tradingAddress,
                          showDateField = true
                        )
                      )
                    )
                  case ValidForm(_, data) => {
                    for {
                      _ <- updateDataStrict[TradingPremises](index) { tp =>
                        tp.copy(status = Some(StatusConstants.Deleted), endDate = Some(data), hasChanged = true)
                      }
                    } yield Redirect(routes.YourTradingPremisesController.get(complete))
                  }
                }

              case _ => removeWithoutDate
            }
          }
      }
  }
}