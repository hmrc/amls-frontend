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

package services.flowmanagement.flowrouters.businessmatching

import javax.inject.{Inject, Singleton}
import models.flowmanagement._
import play.api.mvc.Result
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import services.flowmanagement.pagerouters.addflow._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessTypeRouter @Inject()(val businessMatchingService: BusinessMatchingService,
                                      val addMoreActivitiesPageRouter: AddMoreBusinessTypesPageRouter,
                                      val businessAppliedForPSRNumberPageRouter: BusinessAppliedForPsrNumberPageRouter,
                                      val fitAndProperPageRouter: FitAndProperPageRouter,
                                      val newServicesInformationPageRouter: NeedMoreInformationPageRouter,
                                      val noPSRPageRouter: NoPSRPageRouter,
                                      val selectActivitiesPageRouter: SelectBusinessTypesPageRouter,
                                      val subServicesPageRouter: SubSectorsPageRouter,
                                      val tradingPremisesPageRouter: TradingPremisesPageRouter,
                                      val updateServicesSummaryPageRouter: AddBusinessTypeSummaryPageRouter,
                                      val whatDoYouDoHerePageRouter: WhatDoYouDoHerePageRouter,
                                      val whichFitAndProperPageRouter: WhichFitAndProperPageRouter,
                                      val whichTradingPremisesPageRouter: WhichTradingPremisesPageRouter
                                     ) extends Router[AddBusinessTypeFlowModel] {


  override def getRoute(credId: String, pageId: PageId, model: AddBusinessTypeFlowModel, edit: Boolean = false)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    pageId match {
      case AddMoreBusinessTypesPageId => addMoreActivitiesPageRouter.getRoute(credId, model, edit)
      case PsrNumberPageId => businessAppliedForPSRNumberPageRouter.getRoute(credId, model, edit)
      case FitAndProperPageId => fitAndProperPageRouter.getRoute(credId, model, edit)
      case NeedMoreInformationPageId => newServicesInformationPageRouter.getRoute(credId, model, edit)
      case NoPSRPageId => noPSRPageRouter.getRoute(credId, model, edit)
      case SelectBusinessTypesPageId => selectActivitiesPageRouter.getRoute(credId, model, edit)
      case SubSectorsPageId => subServicesPageRouter.getRoute(credId, model, edit)
      case TradingPremisesPageId => tradingPremisesPageRouter.getRoute(credId, model, edit)
      case AddBusinessTypeSummaryPageId => updateServicesSummaryPageRouter.getRoute(credId, model, edit)
      case WhatDoYouDoHerePageId => whatDoYouDoHerePageRouter.getRoute(credId, model, edit)
      case WhichFitAndProperPageId => whichFitAndProperPageRouter.getRoute(credId, model, edit)
      case WhichTradingPremisesPageId => whichTradingPremisesPageRouter.getRoute(credId, model, edit)
    }
  }
}
