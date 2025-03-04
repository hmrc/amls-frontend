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

package services.flowmanagement.flowrouters.businessmatching

import javax.inject.{Inject, Singleton}
import models.flowmanagement._
import play.api.mvc.Result
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import services.flowmanagement.pagerouters.removeflow._
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessTypeRouter @Inject() (
  val businessMatchingService: BusinessMatchingService,
  val whatServicesToRemovePageRouter: WhatBusinessTypesToRemovePageRouter,
  val needToUpdatePageRouter: NeedToUpdatePageRouter,
  val removeServicesSummaryPageRouter: RemoveBusinessTypesSummaryPageRouter,
  val unableToRemovePageRouter: UnableToRemovePageRouter,
  val whatDateToRemovePageRouter: WhatDateToRemovePageRouter
) extends Router[RemoveBusinessTypeFlowModel] {

  override def getRoute(credId: String, pageId: PageId, model: RemoveBusinessTypeFlowModel, edit: Boolean = false)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] =
    pageId match {
      case WhatBusinessTypesToRemovePageId  => whatServicesToRemovePageRouter.getRoute(credId, model, edit)
      case NeedToUpdatePageId               => needToUpdatePageRouter.getRoute(credId, model, edit)
      case RemoveBusinessTypesSummaryPageId => removeServicesSummaryPageRouter.getRoute(credId, model, edit)
      case UnableToRemovePageId             => unableToRemovePageRouter.getRoute(credId, model, edit)
      case WhatDateRemovedPageId            => whatDateToRemovePageRouter.getRoute(credId, model, edit)
      case _                                => throw new Exception("PagId not in remove flow")
    }
}
