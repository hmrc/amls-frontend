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
import controllers.BaseController
import controllers.businessmatching.updateservice.UpdateServiceHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessActivities
import models.businessmatching.updateservice.TradingPremisesActivities
import models.flowmanagement.{AddServiceFlowModel, WhichTradingPremisesPageId}
import models.tradingpremises.TradingPremises
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, StatusConstants}
import views.html.businessmatching.updateservice.add.which_trading_premises

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class WhichTradingPremisesController @Inject()(
                                                val authConnector: AuthConnector,
                                                implicit val dataCacheConnector: DataCacheConnector,
                                                val statusService: StatusService,
                                                val businessMatchingService: BusinessMatchingService,
                                                val helper: UpdateServiceHelper,
                                                val router: Router[AddServiceFlowModel]
                                              ) extends BaseController with RepeatingSection {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getFormData map { case (flowModel, activity, tradingPremises) =>
          val form = flowModel.tradingPremisesActivities.fold[Form2[TradingPremisesActivities]](EmptyForm)(Form2[TradingPremisesActivities])

          Ok(which_trading_premises(form, edit, tradingPremises, BusinessActivities.getValue(activity))
          )
        } getOrElse InternalServerError("Cannot retrieve form data")
  }

  private def getFormData(implicit hc: HeaderCarrier, ac: AuthContext) = for {
    flowModel <- OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key))
    activity <- OptionT.fromOption[Future](flowModel.activity)
    tradingPremises <- OptionT.liftF(tradingPremises)
  } yield (flowModel, activity, tradingPremises)

  private def tradingPremises(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[(TradingPremises, Int)]] =
    getData[TradingPremises].map {
      _.zipWithIndex.filterNot { case (tp, _) =>
        tp.status.contains(StatusConstants.Deleted) | !tp.isComplete
      }
    }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[TradingPremisesActivities](request.body) match {
          case f: InvalidForm => getFormData map { case (_, activity, tradingPremises) =>
            BadRequest(which_trading_premises(f, edit, tradingPremises, BusinessActivities.getValue(activity)))
          } getOrElse InternalServerError("Cannot retrieve form data")

          case ValidForm(_, data) =>
            dataCacheConnector.update[AddServiceFlowModel](AddServiceFlowModel.key) {
              case Some(model) => model.tradingPremisesActivities(Some(data))
          } flatMap {
            case Some(model) => router.getRoute(WhichTradingPremisesPageId, model)
            case _ => Future.successful(InternalServerError("Cannot retrieve form data"))
          }
        }
  }
}