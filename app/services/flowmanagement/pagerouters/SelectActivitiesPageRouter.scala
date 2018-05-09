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

package services.flowmanagement.pagerouters

import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement.AddServiceFlowModel
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.PageRouter
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class SelectActivitiesPageRouter @Inject()(val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService) extends PageRouter[AddServiceFlowModel] {

  override def getPageRoute(model: AddServiceFlowModel, edit: Boolean = false)
                           (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    if (edit && model.areNewActivitiesAtTradingPremises.isDefined) {
      Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))
    } else {
      model.activity match {
        case Some(TrustAndCompanyServices) => Future.successful(Redirect(addRoutes.FitAndProperController.get()))
        case Some(MoneyServiceBusiness) => Future.successful(Redirect(addRoutes.TradingPremisesController.get()))
        case _ => Future.successful(Redirect(addRoutes.TradingPremisesController.get()))
      }
    }
  }
}


