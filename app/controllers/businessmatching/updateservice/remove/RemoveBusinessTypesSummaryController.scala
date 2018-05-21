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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import forms.EmptyForm
import javax.inject.Inject
import models.flowmanagement.{RemoveBusinessTypeFlowModel, RemoveBusinessTypesSummaryPageId}
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.remove.remove_activities_summary

import scala.concurrent.Future

class RemoveBusinessTypesSummaryController @Inject()(
                                                   val authConnector: AuthConnector,
                                                   val dataCacheConnector: DataCacheConnector,
                                                   val helper: RemoveBusinessTypeHelper,
                                                   val router: Router[RemoveBusinessTypeFlowModel]
                                                 ) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request => {
        for {
          flow <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](RemoveBusinessTypeFlowModel.key))
        } yield Ok(remove_activities_summary(EmptyForm, flow))
      } getOrElse InternalServerError("Unable to get the flow model")
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request => {
        for {
          model <- updateSubscription
          route <- OptionT.liftF(router.getRoute(RemoveBusinessTypesSummaryPageId, model))
        } yield route
      } getOrElse InternalServerError("Unable to remove the business type")
  }

  private def updateSubscription()
                                (implicit ac: AuthContext, hc: HeaderCarrier): OptionT[Future, RemoveBusinessTypeFlowModel] = for {
    model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](RemoveBusinessTypeFlowModel.key))
    _ <- helper.removeFitAndProper(model)
    _ <- helper.removeBusinessMatchingBusinessTypes(model)
    _ <- helper.removeTradingPremisesBusinessTypes(model)
    _ <- helper.removeSectionData(model)
  } yield model
}
