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

import cats.data.OptionT
import cats.implicits._
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement._
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Redirect}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VariationAddServiceRouter @Inject()(val businessMatchingService: BusinessMatchingService) extends Router[AddServiceFlowModel] {



  // scalastyle:off cyclomatic.complexity
  // scalastyle:off method.length
  override def getRoute(pageId: PageId, model: AddServiceFlowModel, edit: Boolean = false)
                       (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = pageId match {

    case SelectActivitiesPageId if edit && model.areNewActivitiesAtTradingPremises.isDefined =>
      Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))

    case SelectActivitiesPageId => {
      model.activity match {
        case Some(TrustAndCompanyServices) => Future.successful(Redirect(addRoutes.FitAndProperController.get(edit)))
        case Some(MoneyServiceBusiness) => Future.successful(Redirect(addRoutes.TradingPremisesController.get()))
        case _ => Future.successful(Redirect(addRoutes.TradingPremisesController.get(edit)))
      }
    }

//fit and proper pages
    case FitAndProperPageId if edit && model.responsiblePeople.isDefined =>
      Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))

    case FitAndProperPageId =>
      (model.fitAndProper, edit) match {
        case (Some(true), _) => Future.successful(Redirect(addRoutes.WhichFitAndProperController.get(edit)))
        case (Some(false), true) => Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))
        case (Some(false), false) => Future.successful(Redirect(addRoutes.TradingPremisesController.get(edit)))
      }

    case WhichFitAndProperPageId =>
      edit match {
        case true => Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))
        case false => Future.successful(Redirect(addRoutes.TradingPremisesController.get(edit)))
      }

    case WhichFitAndProperPageId =>
      Future.successful(Redirect(addRoutes.TradingPremisesController.get(edit)))

//trading premises pages
    case TradingPremisesPageId if edit && model.tradingPremisesActivities.isDefined =>
      Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))

    case TradingPremisesPageId =>
      model.areNewActivitiesAtTradingPremises match {
        case Some(true) =>
          Future.successful(Redirect(addRoutes.WhichTradingPremisesController.get(edit)))
        case _ =>
          Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))
      }

    case WhichTradingPremisesPageId =>
      Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))

//update service page
    case UpdateServiceSummaryPageId =>
      businessMatchingService.getRemainingBusinessActivities flatMap {
        case set if set.nonEmpty =>
          OptionT.some(Redirect(addRoutes.AddMoreActivitiesController.get()))
        case _ =>
          newServiceInformationRedirect
      } getOrElse error(pageId)

    case AddMoreAcivitiesPageId =>
      model.addMoreActivities match {
        case Some(true) =>
          Future.successful(Redirect(addRoutes.SelectActivitiesController.get(edit)))

        case _ =>
          newServiceInformationRedirect getOrElse error(pageId)
      }

    case NewServiceInformationPageId =>
      Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
  }

  private def error(pageId: PageId) = InternalServerError(s"Failed to get route from $pageId")

  private def newServiceInformationRedirect(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext) =
    businessMatchingService.getAdditionalBusinessActivities map { activities =>
      if (!activities.forall {
        case BillPaymentServices | TelephonePaymentService => true
        case _ => false
      }) {
        Redirect(addRoutes.NewServiceInformationController.get())
      } else {
        Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }
}
