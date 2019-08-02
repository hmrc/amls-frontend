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

package services.flowmanagement.pagerouters.addflow

import cats.implicits._
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BillPaymentServices, TelephonePaymentService}
import models.flowmanagement.{AddMoreBusinessTypesPageId, AddBusinessTypeFlowModel, PageId}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Redirect}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.PageRouter
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddMoreBusinessTypesPageRouter @Inject()(val statusService: StatusService,
                                               val businessMatchingService: BusinessMatchingService) extends PageRouter[AddBusinessTypeFlowModel] {

  override def getPageRoute(model: AddBusinessTypeFlowModel, edit: Boolean = false)
                           (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    model.addMoreActivities match {
      case Some(true) =>
        Future.successful(Redirect(addRoutes.SelectBusinessTypeController.get(edit)))
      case _ =>
        newServiceInformationRedirect getOrElse error(AddMoreBusinessTypesPageId)
    }
  }

  override def getPageRouteNewAuth(credId: String, model: AddBusinessTypeFlowModel, edit: Boolean = false)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    model.addMoreActivities match {
      case Some(true) =>
        Future.successful(Redirect(addRoutes.SelectBusinessTypeController.get(edit)))
      case _ =>
        newServiceInformationRedirect(credId) getOrElse error(AddMoreBusinessTypesPageId)
    }
  }

  private def newServiceInformationRedirect(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext) =
    businessMatchingService.getAdditionalBusinessActivities map { activities =>
      if (!activities.forall {
        case BillPaymentServices | TelephonePaymentService => true
        case _ => false
      }) {
        Redirect(addRoutes.NeedMoreInformationController.get())
      } else {
        Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

  private def newServiceInformationRedirect(credId:String)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    businessMatchingService.getAdditionalBusinessActivities(credId) map { activities =>
      if (!activities.forall {
        case BillPaymentServices | TelephonePaymentService => true
        case _ => false
      }) {
        Redirect(addRoutes.NeedMoreInformationController.get())
      } else {
        Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

}


