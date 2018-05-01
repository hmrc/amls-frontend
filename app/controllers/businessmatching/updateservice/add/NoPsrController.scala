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
import forms.EmptyForm
import javax.inject.{Inject, Singleton}
import models.businessmatching.MoneyServiceBusiness
import models.flowmanagement.{AddServiceFlowModel, NoPSRPageId}
import models.status.{NotCompleted, SubmissionReady}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.add.cannot_add_services

@Singleton
class NoPsrController @Inject()(
                                 val authConnector: AuthConnector,
                                 implicit val dataCacheConnector: DataCacheConnector,
                                 val statusService: StatusService,
                                 val businessMatchingService: BusinessMatchingService,
                                 val helper: UpdateServiceHelper,
                                 val router: Router[AddServiceFlowModel]
                               ) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.getStatus map {
          case NotCompleted | SubmissionReady => Ok(cannot_add_services(EmptyForm))
          case _ => Ok(cannot_add_services(EmptyForm))
        }
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          _ <- OptionT(dataCacheConnector.update[AddServiceFlowModel](AddServiceFlowModel.key)(_ =>
            AddServiceFlowModel(activity = Some(MoneyServiceBusiness))))
          model <- OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key))
          route <- OptionT.liftF(router.getRoute(NoPSRPageId, model))
        } yield route) getOrElse InternalServerError("Could not get the flow model")
  }
}
