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

package services.flowmanagement.routings

import controllers.businessmatching.updateservice.routes
import models.businessmatching._
import models.businessmatching.updateservice._
import models.flowmanagement._
import play.api.mvc.Result
import play.api.mvc.Results.Redirect


object VariationAddServiceRouting {

   def getRoute(pageId: PageId, model: AddServiceFlowModel): Result = pageId match {
    case WhatDoYouWantToDoPageId => Redirect(routes.ChangeServicesController.get())

    case BusinessActivitiesSelectionPageId =>
      Redirect(routes.TradingPremisesController.get(0))

    case TradingPremisesDecisionPageId => {
      model.areNewActivitiesAtTradingPremises match {
        case Some(NewActivitiesAtTradingPremisesYes(_)) =>
          Redirect(routes.WhichTradingPremisesController.get(0))
        case _ =>
          Redirect(routes.UpdateServicesSummaryController.get())
      }
    }

    case TradingPremisesSelectionPageId => {
      Redirect(routes.UpdateServicesSummaryController.get())
    }

    case AddServiceSummaryPageId =>
      val informationRequired: Boolean = model.businessActivities exists { activities =>
        activities.businessActivities.intersect(Set(HighValueDealing, MoneyServiceBusiness, TrustAndCompanyServices, EstateAgentBusinessService, AccountancyServices)).nonEmpty
      }

      if (informationRequired) {
        Redirect(routes.NewServiceInformationController.get())
      } else {
        Redirect(controllers.routes.RegistrationProgressController.get())
      }

    case NewServiceInformationPageId =>
      Redirect(controllers.routes.RegistrationProgressController.get())
  }
}
