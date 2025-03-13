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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview, SubmissionStatus}
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.cache.Cache
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.tradingpremises.YourTradingPremisesView

import scala.concurrent.Future

@Singleton
class YourTradingPremisesController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  view: YourTradingPremisesView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  private def updateTradingPremises(
    tradingPremises: Option[Seq[TradingPremises]]
  ): Future[Option[Seq[TradingPremises]]] =
    tradingPremises match {
      case Some(tpSeq) =>
        val updatedList = tpSeq.filterEmpty.map { premises =>
          premises.copy(hasAccepted = true)
        }
        Future.successful(Some(updatedList))
      case _           => Future.successful(tradingPremises)
    }

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
      tp     <- dataCacheConnector.fetchAll(request.credId) map { cache =>
                  for {
                    c: Cache <- cache
                    tp       <- c.getEntry[Seq[TradingPremises]](TradingPremises.key)
                  } yield tp
                }
    } yield (tp, status)) map {
      case (Some(data), status) =>
        val (completeTp, incompleteTp) = TradingPremises
          .filterWithIndex(data)
          .partition(_._1.isComplete)
        Ok(view(edit, status, completeTp, incompleteTp))
      case _                    => Redirect(controllers.routes.RegistrationProgressController.get())
    }
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    (for {
      tp    <- dataCacheConnector.fetch[Seq[TradingPremises]](request.credId, TradingPremises.key)
      tpNew <- updateTradingPremises(tp)
      _     <-
        dataCacheConnector.save[Seq[TradingPremises]](request.credId, TradingPremises.key, tpNew.getOrElse(Seq.empty))
    } yield Redirect(controllers.routes.RegistrationProgressController.get())) recoverWith { case _: Throwable =>
      Future.successful(InternalServerError("Unable to save data and get redirect link"))
    }
  }

  def post(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    for {
      _ <- updateDataStrict[TradingPremises](request.credId, index) { tp =>
             tp.copy(hasAccepted = true, hasChanged = true)
           }
    } yield Redirect(controllers.tradingpremises.routes.YourTradingPremisesController.get())
  }

  def answers: Action[AnyContent] = get(true)

  def getIndividual(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      cache <- OptionT(dataCacheConnector.fetchAll(request.credId))
      bm    <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
      tp    <- OptionT.fromOption[Future](getData[TradingPremises](cache, index))
    } yield edit match {
      case true if tp.isComplete & tp.hasAccepted                            =>
        Redirect(controllers.tradingpremises.routes.CheckYourAnswersController.get(index))
      case true if !tp.isComplete & ControllerHelper.isMSBSelected(Some(bm)) =>
        Redirect(controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(index))
      case true if !tp.isComplete                                            =>
        Redirect(controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(index))
      case _                                                                 => Redirect(controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(index))
    }).getOrElse(NotFound(notFoundView))
  }
}

object ModelHelpers {
  implicit class removeUrl(model: TradingPremises) {

    private def isSubmission(status: SubmissionStatus) =
      Set(NotCompleted, SubmissionReady, SubmissionReadyForReview).contains(status)

    def removeUrl(index: Int, complete: Boolean = false, status: SubmissionStatus): String =
      model.registeringAgentPremises match {
        case Some(RegisteringAgentPremises(true)) if !isSubmission(status) && model.lineId.isDefined =>
          controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(index, complete).url
        case _                                                                                       =>
          controllers.tradingpremises.routes.RemoveTradingPremisesController.get(index, complete).url
      }
  }
}
