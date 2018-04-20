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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.UpdateServiceHelper
import javax.inject.{Inject, Singleton}
import models.flowmanagement.AddServiceFlowModel
import models.status
import models.status.{NotCompleted, SubmissionReady}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

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
    implicit authContext => implicit request =>
      statusService.getStatus map {
        case NotCompleted | SubmissionReady => Ok(views.html.businessmatching.cannot_continue_with_the_application())
        case _ => Ok(views.html.businessmatching.cannot_add_services())
      }
  }
}
