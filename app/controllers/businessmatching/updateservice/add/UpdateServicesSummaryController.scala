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
import forms.EmptyForm
import javax.inject.{Inject, Singleton}
import models.flowmanagement.{AddServiceFlowModel, UpdateServiceSummaryPageId}
import services.TradingPremisesService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

@Singleton
class UpdateServicesSummaryController @Inject()(
                                                 val authConnector: AuthConnector,
                                                 implicit val dataCacheConnector: DataCacheConnector,
                                                 val tradingPremisesService: TradingPremisesService,
                                                 val updateServicesSummaryControllerHelper: UpdateServicesSummaryControllerHelper,
                                                 val router: Router[AddServiceFlowModel]
                                               ) extends BaseController with RepeatingSection {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key)) map { model =>
          Ok(views.html.businessmatching.updateservice.add.update_services_summary(EmptyForm, model))
        } getOrElse InternalServerError("Unable to get the flow model")
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key))
          activity <- OptionT.fromOption[Future](model.activity)
          _ <- updateServicesSummaryControllerHelper.updateTradingPremises(model)
          _ <- OptionT(updateServicesSummaryControllerHelper.updateBusinessMatching(activity))
          _ <- OptionT(updateServicesSummaryControllerHelper.updateServicesRegister(activity))
          _ <- updateServicesSummaryControllerHelper.updateHasAcceptedFlag(model)
          route <- OptionT.liftF(router.getRoute(UpdateServiceSummaryPageId, model))
        } yield {
          route
        }) getOrElse InternalServerError("Could not fetch the flow model")
  }
}