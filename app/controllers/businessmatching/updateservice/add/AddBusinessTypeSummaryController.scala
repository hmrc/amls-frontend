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
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.EmptyForm
import javax.inject.{Inject, Singleton}
import models.flowmanagement.{AddBusinessTypeFlowModel, AddBusinessTypeSummaryPageId}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import services.{StatusService, TradingPremisesService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.businessmatching.updateservice.add.update_services_summary

import scala.concurrent.Future

@Singleton
class AddBusinessTypeSummaryController @Inject()(
                                                 val authConnector: AuthConnector,
                                                 implicit val dataCacheConnector: DataCacheConnector,
                                                 val statusService: StatusService,
                                                 val businessMatchingService: BusinessMatchingService,
                                                 val helper: AddBusinessTypeHelper,
                                                 val router: Router[AddBusinessTypeFlowModel],
                                                 val tradingPremisesService: TradingPremisesService
                                               ) extends BaseController with RepeatingSection {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](AddBusinessTypeFlowModel.key)) collect {
          case model if model != AddBusinessTypeFlowModel() => Ok(update_services_summary(EmptyForm, model))
        } getOrElse Redirect(controllers.businessmatching.routes.SummaryController.get())
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](AddBusinessTypeFlowModel.key))
          activity <- OptionT.fromOption[Future](model.activity)
                  _ <- helper.updateTradingPremises(model)
                  _ <- helper.updateResponsiblePeople(model)
                  _ <- helper.updateSupervision
                  _ <- helper.updateBusinessMatching(model)
                  _ <- helper.updateServicesRegister(model)
                  _ <- helper.updateBusinessActivities(model)
                  _ <- helper.updateHasAcceptedFlag(model)
                  _ <- helper.clearFlowModel()
          route <- OptionT.liftF(router.getRoute(AddBusinessTypeSummaryPageId, model))
        } yield {
          route
        }) getOrElse InternalServerError("Could not fetch the flow model")
  }
}