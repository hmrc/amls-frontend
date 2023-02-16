/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.status._
import models.tradingpremises.{ActivityEndDate, TradingPremises}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import utils.{AuthAction, RepeatingSection, StatusConstants}
import views.html.tradingpremises.remove_trading_premises


class RemoveTradingPremisesController @Inject () (
                                                   val dataCacheConnector: DataCacheConnector,
                                                   val authAction: AuthAction,
                                                   val ds: CommonPlayDependencies,
                                                   val statusService: StatusService,
                                                   val cc: MessagesControllerComponents,
                                                   remove_trading_premises: remove_trading_premises,
                                                   implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, complete: Boolean = false) = authAction.async {
    implicit request =>
      for {
        tp <- getData[TradingPremises](request.credId, index)
        status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
      } yield (tp, status) match {

        case (Some(_), SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_)) =>
          Ok (remove_trading_premises (
              f = EmptyForm,
              index = index,
              complete = complete,
              tradingAddress = tp.yourTradingPremises.fold("")(_.tradingPremisesAddress.toLines.mkString(", ")),
              showDateField = tp.lineId.isDefined
            )
          )

        case (Some(_), _) => Ok (
          remove_trading_premises (
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

  def remove(index: Int, complete: Boolean = false) = authAction.async {
    implicit request =>

      def removeWithoutDate = removeDataStrict[TradingPremises](request.credId, index) map { _ =>
        Redirect(routes.YourTradingPremisesController.get())
      }

      statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId).flatMap {
        case NotCompleted | SubmissionReady => removeDataStrict[TradingPremises](request.credId, index) map { _ =>
          Redirect(routes.YourTradingPremisesController.get(complete))
        }
        case SubmissionReadyForReview => {
          getData[TradingPremises](request.credId, index) flatMap { premises =>
            premises.lineId match {
              case Some(_) => {
                for {
                  _ <- updateDataStrict[TradingPremises](request.credId, index) { tp =>
                    tp.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
                  }
                } yield Redirect(routes.YourTradingPremisesController.get(complete))
              }
              case _ => removeWithoutDate
            }
          }
        }
        case _ => {
          getData[TradingPremises](request.credId, index) flatMap { premises =>
            premises.lineId match {
              case Some(tp) =>
                val extraFields = Map(
                  "premisesStartDate" -> Seq(premises.get.yourTradingPremises.get.startDate.get.toString("yyyy-MM-dd"))
                )
                Form2[ActivityEndDate](request.body.asFormUrlEncoded.get ++ extraFields) match {
                  case f: InvalidForm =>
                    for {
                      tp <- getData[TradingPremises](request.credId, index)
                    } yield (tp) match {
                      case (Some(_)) =>
                        BadRequest(
                          remove_trading_premises(
                            f = f,
                            index = index,
                            complete = complete,
                            tradingAddress = tp.yourTradingPremises.fold("")(_.tradingPremisesAddress.toLines.mkString(", ")),
                            showDateField = true
                          )
                        )
                      case _ => NotFound(notFoundView)
                    }
                  case ValidForm(_, data) => {
                    for {
                      _ <- updateDataStrict[TradingPremises](request.credId, index) { tp =>
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
}