/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.tradingpremises.RemoveTradingPremisesFormProvider
import models.status._
import models.tradingpremises.{TradingPremises, YourTradingPremises}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.StatusService
import utils.{AuthAction, RepeatingSection, StatusConstants}
import views.html.tradingpremises.RemoveTradingPremisesView

import java.time.format.DateTimeFormatter.ofPattern
import scala.concurrent.Future

class RemoveTradingPremisesController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val cc: MessagesControllerComponents,
  formProvider: RemoveTradingPremisesFormProvider,
  view: RemoveTradingPremisesView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, complete: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    for {
      tp     <- getData[TradingPremises](request.credId, index)
      status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
    } yield (tp, status) match {

      case (Some(_), SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_)) =>
        Ok(
          view(
            formProvider(),
            index = index,
            complete = complete,
            tradingAddress = tp.yourTradingPremises.fold("")(_.tradingPremisesAddress.toLines.mkString(", ")),
            showDateField = tp.lineId.isDefined
          )
        )

      case (Some(_), _) =>
        Ok(
          view(
            formProvider(),
            index = index,
            complete = complete,
            tradingAddress = tp.yourTradingPremises.fold("")(_.tradingPremisesAddress.toLines.mkString(",")),
            showDateField = false
          )
        )

      case _ => NotFound(notFoundView)
    }
  }

  def remove(index: Int, complete: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    def removeWithoutDate(): Future[Result] = removeDataStrict[TradingPremises](request.credId, index) map { _ =>
      Redirect(routes.YourTradingPremisesController.get())
    }

    statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId).flatMap {
      case NotCompleted | SubmissionReady =>
        removeDataStrict[TradingPremises](request.credId, index) map { _ =>
          Redirect(routes.YourTradingPremisesController.get(complete))
        }
      case SubmissionReadyForReview       =>
        getData[TradingPremises](request.credId, index) flatMap { premises =>
          premises.lineId match {
            case Some(_) =>
              for {
                _ <- updateDataStrict[TradingPremises](request.credId, index) { tp =>
                       tp.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
                     }
              } yield Redirect(routes.YourTradingPremisesController.get(complete))
            case _       => removeWithoutDate()
          }
        }
      case _                              =>
        getData[TradingPremises](request.credId, index) flatMap { premises =>
          premises.lineId match {
            case Some(tp) =>
              premises.yourTradingPremises
                .map { case YourTradingPremises(_, tradingPremisesAddress, _, Some(startDate), _) =>
                  formProvider()
                    .bindFromRequest()
                    .fold(
                      formWithErrors =>
                        for {
                          tp <- getData[TradingPremises](request.credId, index)
                        } yield tp match {
                          case (Some(_)) =>
                            BadRequest(
                              view(
                                form = formWithErrors,
                                index = index,
                                complete = complete,
                                tradingAddress = tradingPremisesAddress.toLines.mkString(", "),
                                showDateField = true
                              )
                            )
                          case _         => NotFound(notFoundView)
                        },
                      data =>
                        if (data.endDate.isBefore(startDate)) {

                          val formWithError = formProvider()
                            .fill(data)
                            .withError(
                              "endDate",
                              "error.expected.tp.date.after.start",
                              startDate.format(ofPattern("dd-MM-yyyy"))
                            )

                          Future.successful(
                            BadRequest(
                              view(
                                form = formWithError,
                                index = index,
                                complete = complete,
                                tradingAddress = tradingPremisesAddress.toLines.mkString(", "),
                                showDateField = true
                              )
                            )
                          )
                        } else {
                          updateDataStrict[TradingPremises](request.credId, index) { tp =>
                            tp.copy(status = Some(StatusConstants.Deleted), endDate = Some(data), hasChanged = true)
                          }.map { _ =>
                            Redirect(routes.YourTradingPremisesController.get(complete))
                          }
                        }
                    )
                }
                .getOrElse(throw new RuntimeException("Could not access trading premises"))
            case _        => removeWithoutDate()
          }
        }
    }
  }
}
