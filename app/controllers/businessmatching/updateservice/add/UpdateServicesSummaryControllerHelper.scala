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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessActivity, BusinessMatching}
import models.flowmanagement.AddServiceFlowModel
import models.tradingpremises.TradingPremises
import services.TradingPremisesService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UpdateServicesSummaryControllerHelper @Inject()(
                                                       val authConnector: AuthConnector,
                                                       implicit val dataCacheConnector: DataCacheConnector,
                                                       val tradingPremisesService: TradingPremisesService
                                                     ) extends RepeatingSection
{

  def updateHasAcceptedFlag(model: AddServiceFlowModel)(implicit ac: AuthContext, hc: HeaderCarrier) =
    OptionT.liftF(dataCacheConnector.save[AddServiceFlowModel](AddServiceFlowModel.key, model.copy(hasAccepted = true)))

  def updateServicesRegister(activity: BusinessActivity)(implicit ac: AuthContext, hc: HeaderCarrier): Future[Option[ServiceChangeRegister]] =
    dataCacheConnector.update[ServiceChangeRegister](ServiceChangeRegister.key) {
      case Some(model@ServiceChangeRegister(Some(activities))) =>
        model.copy(addedActivities = Some(activities + activity))
      case _ => ServiceChangeRegister(Some(Set(activity)))
    }

  def updateTradingPremises(model: AddServiceFlowModel)(implicit ac: AuthContext, hc: HeaderCarrier): OptionT[Future, Seq[TradingPremises]] = for {
    tradingPremises <- OptionT.liftF(tradingPremisesData)
    activity <- OptionT.fromOption[Future](model.activity)
    indices <- OptionT.fromOption[Future](model.tradingPremisesActivities map {
      _.index.toSeq
    }) orElse OptionT.some(Seq.empty)
    newTradingPremises <- OptionT.some[Future, Seq[TradingPremises]](
      tradingPremisesService.addBusinessActivtiesToTradingPremises(indices, tradingPremises, activity, false)
    )
    _ <- OptionT.liftF(dataCacheConnector.save[Seq[TradingPremises]](TradingPremises.key, newTradingPremises))
  } yield tradingPremises

  def tradingPremisesData(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[TradingPremises]] =
    getData[TradingPremises].map {
      _.filterNot(tp => tp.status.contains(StatusConstants.Deleted) | !tp.isComplete)
    }

  def updateBusinessMatching(activity: BusinessActivity)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[BusinessMatching]] =
    dataCacheConnector.update[BusinessMatching](BusinessMatching.key) { case Some(bm) =>
      bm.copy(activities = bm.activities map { b => b.copy(businessActivities = b.businessActivities + activity) })
    }
}
