/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.AMLSTurnoverFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.renewal.{AMLSTurnover, Renewal}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.AMLSTurnoverView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AMLSTurnoverController @Inject()(val dataCacheConnector: DataCacheConnector,
                                       val authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       val renewalService: RenewalService,
                                       val cc: MessagesControllerComponents,
                                       formProvider: AMLSTurnoverFormProvider,
                                       view: AMLSTurnoverView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
        (for {
          cache <- optionalCache
          businessMatchingOpt = cache.getEntry[BusinessMatching](BusinessMatching.key)
          businessMatching <- businessMatchingOpt
          form = getForm(businessMatchingOpt)
          activities = businessMatching.alphabeticalBusinessActivitiesLowerCase(false)
        } yield {
          val filledForm = (for {
            renewal <- cache.getEntry[Renewal](Renewal.key)
            turnover <- renewal.turnover
          } yield form.fill(turnover)) getOrElse form
          Ok(view(filledForm, edit, activities))
        }) getOrElse Ok(view(formProvider(), edit, None))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request => {
      dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) flatMap { bm =>
        getForm(bm).bindFromRequest().fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, edit, bm.flatMap(_.alphabeticalBusinessActivitiesLowerCase(false))))
            ),
          data =>
            for {
              renewal <- renewalService.getRenewal(request.credId)
              _ <- renewalService.updateRenewal(request.credId, renewal.turnover(data))
              businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
            } yield {
              if (edit) {
                Redirect(routes.SummaryController.get)
              } else {
                getRouting(ControllerHelper.getBusinessActivity(businessMatching))
              }
            }
        )
      }
    }
  }

  private def getRouting(ba: Option[BusinessActivities]) = {
    ba match {
      case Some(activities) => activities.businessActivities match {
        case x if x.contains(ArtMarketParticipant) => Redirect(routes.AMPTurnoverController.get())
        case x if x.contains(MoneyServiceBusiness) => Redirect(routes.TotalThroughputController.get())
        case x if x.contains(AccountancyServices) => Redirect(routes.CustomersOutsideIsUKController.get())
        case x if x.contains(HighValueDealing) => Redirect(routes.CustomersOutsideIsUKController.get())
        case _ => Redirect(routes.SummaryController.get)
      }
      case _ => InternalServerError("Unable to redirect from Turnover page")
    }
  }

  private def getForm(bm: Option[BusinessMatching]): Form[AMLSTurnover] = {
    (for {
      activities <- bm.alphabeticalBusinessActivitiesLowerCase()
      isSingleActivity = if (activities.length == 1) activities.headOption else None
    } yield {
      formProvider(isSingleActivity)
    }) getOrElse formProvider()
  }
}
