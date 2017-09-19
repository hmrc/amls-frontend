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

package controllers.tradingpremises

import cats.data.OptionT
import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.status._
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{ControllerHelper, RepeatingSection}
import views.html.tradingpremises._

import scala.concurrent.Future
import cats.implicits._
import forms.EmptyForm

trait SummaryController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector
  def statusService: StatusService

  private def updateTradingPremises(tradingPremises: Option[Seq[TradingPremises]]) : Future[Option[Seq[TradingPremises]]] = {
    tradingPremises match {
      case Some(tpSeq) => {
        val updatedList = tpSeq.map { premises =>
          premises.copy(hasAccepted = true)
        }
        Future.successful(Some(updatedList))
      }
      case _ => Future.successful(tradingPremises)
    }
  }

  def get(edit:Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        status <- statusService.getStatus
        tp <- dataCacheConnector.fetchAll map {
          cache =>
            for {
              c: CacheMap <- cache
              tp <- c.getEntry[Seq[TradingPremises]](TradingPremises.key)
            } yield tp
        }
      } yield (tp, status)) map {
        case (Some(data), status) => Ok(summary(EmptyForm, data, edit, status))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        tp <- dataCacheConnector.fetch[Seq[TradingPremises]](TradingPremises.key)
        tpNew <- updateTradingPremises(tp)
        _ <- dataCacheConnector.save[Seq[TradingPremises]](TradingPremises.key, tpNew.getOrElse(Seq.empty))
      } yield Redirect(controllers.routes.RegistrationProgressController.get())) recoverWith {
        case _: Throwable => Future.successful(InternalServerError("Unable to save data and get redirect link"))
      }
  }

  def answers = get(true)

  def getIndividual(index: Int) = Authorised.async {
    implicit authContext => implicit request =>

      (for {
        cache <- OptionT(dataCacheConnector.fetchAll)
        tp <- OptionT.fromOption[Future](getData[TradingPremises](cache, index))
        bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
        activities <- OptionT.fromOption[Future](bm.activities)
      } yield {
        val hasOneService = activities.businessActivities.size == 1
        Ok(summary_details(tp, ControllerHelper.isMSBSelected(Some(bm)), index, hasOneService))
      }).getOrElse(NotFound(notFoundView))
  }
}

object ModelHelpers {
  implicit class removeUrl(model: TradingPremises) {

    private def isSubmission(status: SubmissionStatus) = Set(NotCompleted, SubmissionReady, SubmissionReadyForReview).contains(status)

    def removeUrl(index: Int, complete: Boolean = false, status: SubmissionStatus): String = model.registeringAgentPremises match {
      case Some(RegisteringAgentPremises(true)) if ApplicationConfig.release7 && !isSubmission(status) && model.lineId.isDefined =>
        controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(index, complete).url
      case _ =>
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(index, complete).url
    }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
  // $COVERAGE-ON$
}

