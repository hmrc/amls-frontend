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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.EmptyForm
import javax.inject.{Inject, Singleton}
import models.flowmanagement.{AddBusinessTypeFlowModel, AddBusinessTypeSummaryPageId}
import models.responsiblepeople.ResponsiblePerson
import models.tradingpremises.TradingPremises
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import services.{StatusService, TradingPremisesService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, RepeatingSection, StatusConstants}
import views.html.businessmatching.updateservice.add.update_services_summary

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@Singleton
class AddBusinessTypeSummaryController @Inject()(
                                                  authAction: AuthAction,
                                                  implicit val dataCacheConnector: DataCacheConnector,
                                                  val statusService: StatusService,
                                                  val businessMatchingService: BusinessMatchingService,
                                                  val helper: AddBusinessTypeHelper,
                                                  val router: Router[AddBusinessTypeFlowModel],
                                                  val tradingPremisesService: TradingPremisesService
                                               ) extends DefaultBaseController with RepeatingSection {

  def get() = authAction.async {
    implicit request =>
      (for {
        flowModel: AddBusinessTypeFlowModel <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key))
        filteredTPs: Seq[(TradingPremises, Int)] <- filteredTps(request.credId, flowModel)
        filteredRPs: Seq[(ResponsiblePerson, Int)] <- filteredRps(request.credId, flowModel)
      } yield {
        Ok(update_services_summary(EmptyForm, flowModel, filteredTPs, filteredRPs))
      }) getOrElse Redirect(controllers.businessmatching.routes.SummaryController.get())
  }

  def post() = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key))
          activity <- OptionT.fromOption[Future](model.activity)
                  _ <- helper.updateTradingPremises(request.credId, model)
                  _ <- helper.updateResponsiblePeople(request.credId, model)
                  _ <- helper.updateSupervision(request.credId)
                  _ <- helper.updateBusinessMatching(request.credId, model)
                  _ <- helper.updateServicesRegister(request.credId, model)
                  _ <- helper.updateBusinessActivities(request.credId, model)
                  _ <- helper.updateHasAcceptedFlag(request.credId, model)
                  _ <- helper.clearFlowModel(request.credId)
          route <- OptionT.liftF(router.getRoute(request.credId, AddBusinessTypeSummaryPageId, model))
        } yield {
          route
        }) getOrElse InternalServerError("Could not fetch the flow model")
  }

  private def getTp(credId: String, abtfm: AddBusinessTypeFlowModel)(implicit hc: HeaderCarrier) = for {
    tradingPremises <- OptionT.liftF(tradingPremises(credId))
    indexes: Set[Int] <- OptionT.fromOption[Future](abtfm.tradingPremisesActivities.map(tpa => tpa.index))
  } yield (indexes, tradingPremises)

  private def getRp(credId: String, abtfm: AddBusinessTypeFlowModel)(implicit hc: HeaderCarrier) = {
    for {
      responsiblePeople <- OptionT.liftF(responsiblePeople(credId))
      indexes: Set[Int] <- OptionT.fromOption[Future](abtfm.responsiblePeople.map(rpf => rpf.index))
    } yield (indexes, responsiblePeople)

  }

  private def tradingPremises(credId: String)(implicit hc: HeaderCarrier): Future[Seq[(TradingPremises, Int)]] =
    getData[TradingPremises](credId).map {
      _.zipWithIndex.filterNot { case (tp, _) =>
        tp.status.contains(StatusConstants.Deleted) | !tp.isComplete
      }
    }
  private def responsiblePeople(credId: String)(implicit hc: HeaderCarrier): Future[Seq[(ResponsiblePerson, Int)]] = {
    getData[ResponsiblePerson](credId).map {
      _.zipWithIndex.filterNot { case (rp, _) =>
        rp.status.contains(StatusConstants.Deleted) | !rp.isComplete
      }
    }
  }

  def filteredTps(credId: String, abtfm: AddBusinessTypeFlowModel)(implicit hc: HeaderCarrier) = {
  for {
      (index: Set[Int], tps: Seq[(TradingPremises, Int)]) <- getTp(credId, abtfm)
      filteredTps <- OptionT.pure[Future, Seq[(TradingPremises, Int)]](tps.filter { ele => index.contains(ele._2)})
    } yield filteredTps
  }

  def filteredRps(credId: String, abtfm: AddBusinessTypeFlowModel)(implicit hc: HeaderCarrier) = {
    for {
      (index: Set[Int], rps: Seq[(ResponsiblePerson, Int)]) <- getRp(credId, abtfm)
      filteredRps <- OptionT.pure[Future, Seq[(ResponsiblePerson, Int)]](rps.filter { ele => index.contains(ele._2)})
    } yield filteredRps
  }
}