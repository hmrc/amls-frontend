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

package services.flowmanagement.flowrouters

import javax.inject.{Inject, Singleton}
import models.flowmanagement._
import play.api.mvc.Result
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import services.flowmanagement.pagerouters.removeflow._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessTypeRouter @Inject()(val businessMatchingService: BusinessMatchingService,
                                         val whatServicesToRemovePageRouter: WhatBusinessTypesToRemovePageRouter,
                                         val needToUpdatePageRouter: NeedToUpdatePageRouter,
                                         val removeServicesSummaryPageRouter: RemoveBusinessTypesSummaryPageRouter,
                                         val unableToRemovePageRouter: UnableToRemovePageRouter,
                                         val whatDateToRemovePageRouter: WhatDateToRemovePageRouter
                                        ) extends Router[RemoveBusinessTypeFlowModel] {


  override def getRoute(pageId: PageId, model: RemoveBusinessTypeFlowModel, edit: Boolean = false)
                       (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    pageId match {
      case WhatBusinessTypesToRemovePageId => whatServicesToRemovePageRouter.getPageRoute(model, edit)
      case NeedToUpdatePageId => needToUpdatePageRouter.getPageRoute(model, edit)
      case RemoveBusinessTypesSummaryPageId => removeServicesSummaryPageRouter.getPageRoute(model, edit)
      case UnableToRemovePageId => unableToRemovePageRouter.getPageRoute(model, edit)
      case WhatDateRemovedPageId => whatDateToRemovePageRouter.getPageRoute(model, edit)
      case _ => throw new Exception("PagId not in remove flow")
    }
  }
}
