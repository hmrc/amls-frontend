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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessActivities
import models.businessmatching.updateservice.TradingPremisesActivities
import models.flowmanagement.{AddBusinessTypeFlowModel, WhichTradingPremisesPageId}
import models.tradingpremises.TradingPremises
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, RepeatingSection, StatusConstants}
import views.html.businessmatching.updateservice.add.which_trading_premises

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class WhichTradingPremisesController @Inject()(
                                                authAction: AuthAction,
                                                val ds: CommonPlayDependencies,
                                                implicit val dataCacheConnector: DataCacheConnector,
                                                val statusService: StatusService,
                                                val businessMatchingService: BusinessMatchingService,
                                                val helper: AddBusinessTypeHelper,
                                                val router: Router[AddBusinessTypeFlowModel],
                                                val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        getFormData(request.credId) map { case (flowModel, activity, tradingPremises) =>
          val form = flowModel.tradingPremisesActivities.fold[Form2[TradingPremisesActivities]](EmptyForm)(Form2[TradingPremisesActivities])

          Ok(which_trading_premises(form, edit, tradingPremises, BusinessActivities.getValue(activity))
          )
        } getOrElse InternalServerError("Cannot retrieve form data")
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[TradingPremisesActivities](request.body) match {
          case f: InvalidForm => getFormData(request.credId) map { case (_, activity, tradingPremises) =>
            BadRequest(which_trading_premises(f, edit, tradingPremises, BusinessActivities.getValue(activity)))
          } getOrElse InternalServerError("Cannot retrieve form data")

          case ValidForm(_, data) =>
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              case Some(model) => model.tradingPremisesActivities(Some(data))
          } flatMap {
            case Some(model) => router.getRoute(request.credId, WhichTradingPremisesPageId, model)
            case _ => Future.successful(InternalServerError("Cannot retrieve form data"))
          }
        }
  }

  private def getFormData(credId: String)(implicit hc: HeaderCarrier) = for {
    flowModel <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](credId, AddBusinessTypeFlowModel.key))
    activity <- OptionT.fromOption[Future](flowModel.activity)
    tradingPremises <- OptionT.liftF(tradingPremises(credId))
  } yield (flowModel, activity, tradingPremises)

  private def tradingPremises(credId: String)(implicit hc: HeaderCarrier): Future[Seq[(TradingPremises, Int)]] =
    getData[TradingPremises](credId).map {
      _.zipWithIndex.filterNot { case (tp, _) =>
        tp.status.contains(StatusConstants.Deleted) | !tp.isComplete
      }
    }
}