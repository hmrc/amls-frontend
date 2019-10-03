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
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.flowmanagement.{AddBusinessTypeFlowModel, TradingPremisesPageId}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BooleanFormReadWrite}
import views.html.businessmatching.updateservice.add.trading_premises

import scala.concurrent.Future

@Singleton
class TradingPremisesController @Inject()(
                                           authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           implicit val dataCacheConnector: DataCacheConnector,
                                           val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService,
                                           val helper: AddBusinessTypeHelper,
                                           val router: Router[AddBusinessTypeFlowModel],
                                           val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  val fieldName = "tradingPremisesNewActivities"
  implicit val boolWrite = BooleanFormReadWrite.formWrites(fieldName)
  implicit val boolRead = BooleanFormReadWrite.formRule(fieldName, "error.businessmatching.updateservice.tradingpremisesnewactivities")

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        getFormData(request.credId) map { case (model, activity) =>
          val form = model.areNewActivitiesAtTradingPremises map { v => Form2(v) } getOrElse EmptyForm
          Ok(trading_premises(form, edit, BusinessActivities.getValue(activity)))
        } getOrElse InternalServerError("Unable to show the view")
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[Boolean](request.body) match {
          case form: InvalidForm => getFormData(request.credId) map { case (_, activity) =>
            BadRequest(trading_premises(form, edit, BusinessActivities.getValue(activity)))
          } getOrElse InternalServerError("Unable to show the view")

          case ValidForm(_, data) =>
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              case Some(model) =>model.isActivityAtTradingPremises(Some(data))
                .tradingPremisesActivities(if (data) model.tradingPremisesActivities else None)
            } flatMap {
              case Some(model) => router.getRoute(request.credId, TradingPremisesPageId, model, edit)
              case _ => Future.successful(InternalServerError("Cannot retrieve data"))
            }
        }
  }

  private def getFormData(credId: String)(implicit hc: HeaderCarrier): OptionT[Future, (AddBusinessTypeFlowModel, BusinessActivity)] = for {
    model <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](credId, AddBusinessTypeFlowModel.key))
    activity <- OptionT.fromOption[Future](model.activity)
  } yield (model, activity)

}