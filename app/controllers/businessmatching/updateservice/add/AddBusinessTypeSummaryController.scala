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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.EmptyForm
import javax.inject.{Inject, Singleton}
import models.flowmanagement.{AddBusinessTypeFlowModel, AddBusinessTypeSummaryPageId}
import play.api.mvc.MessagesControllerComponents
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import services.{StatusService, TradingPremisesService}
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching.updateservice.add.update_services_summary

import scala.concurrent.Future

@Singleton
class AddBusinessTypeSummaryController @Inject()(
                                                  authAction: AuthAction,
                                                  val ds: CommonPlayDependencies,
                                                  implicit val dataCacheConnector: DataCacheConnector,
                                                  val statusService: StatusService,
                                                  val businessMatchingService: BusinessMatchingService,
                                                  val helper: AddBusinessTypeHelper,
                                                  val router: Router[AddBusinessTypeFlowModel],
                                                  val tradingPremisesService: TradingPremisesService,
                                                  val cc: MessagesControllerComponents,
                                                  update_services_summary: update_services_summary) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get() = authAction.async {
    implicit request =>
      (for {
        flowModel <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key))
      } yield {
        Ok(update_services_summary(EmptyForm, flowModel))
      }) getOrElse Redirect(controllers.businessmatching.routes.SummaryController.get)
  }

  def post() = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key))
          activity <- OptionT.fromOption[Future](model.activity)
                  _ <- helper.updateSupervision(request.credId)
                  _ <- helper.updateBusinessMatching(request.credId, model)
                  _ <- helper.updateServicesRegister(request.credId, model)
                  _ <- helper.updateBusinessActivities(request.credId, model)
                  _ <- helper.updateHasAcceptedFlag(request.credId, model)
          route <- OptionT.liftF(router.getRoute(request.credId, AddBusinessTypeSummaryPageId, model))
        } yield {
          route
        }) getOrElse InternalServerError("Could not fetch the flow model")
  }
}