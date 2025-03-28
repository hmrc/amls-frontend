/*
 * Copyright 2024 HM Revenue & Customs
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

package services.flowmanagement.pagerouters.removeflow

import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.remove.{routes => removeRoutes}
import javax.inject.{Inject, Singleton}
import models.flowmanagement.RemoveBusinessTypeFlowModel
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.PageRouter
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessTypesSummaryPageRouter @Inject() (
  val statusService: StatusService,
  val businessMatchingService: BusinessMatchingService,
  val dataCacheConnector: DataCacheConnector
) extends PageRouter[RemoveBusinessTypeFlowModel] {

  override def getRoute(credId: String, model: RemoveBusinessTypeFlowModel, edit: Boolean = false)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] =
    Future.successful(Redirect(removeRoutes.NeedMoreInformationController.get()))
}
