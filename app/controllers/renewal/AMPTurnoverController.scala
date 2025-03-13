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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.AMPTurnoverFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing, MoneyServiceBusiness}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.AMPTurnoverView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AMPTurnoverController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val cc: MessagesControllerComponents,
  formProvider: AMPTurnoverFormProvider,
  view: AMPTurnoverView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
      (for {
        cache <- optionalCache
      } yield {
        val form = (for {
          renewal     <- cache.getEntry[Renewal](Renewal.key)
          ampTurnover <- renewal.ampTurnover
        } yield formProvider().fill(ampTurnover)) getOrElse formProvider()
        Ok(view(form, edit))
      }) getOrElse Ok(view(formProvider(), edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            renewal          <- renewalService.getRenewal(request.credId)
            _                <- renewalService.updateRenewal(request.credId, renewal.ampTurnover(data))
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          } yield
            if (edit) {
              Redirect(routes.SummaryController.get)
            } else {
              getRouting(ControllerHelper.getBusinessActivity(businessMatching))
            }
      )
  }

  private def getRouting(ba: Option[BusinessActivities]) =
    ba match {
      case Some(activities) =>
        activities.businessActivities match {
          case x if x.contains(MoneyServiceBusiness) => Redirect(routes.TotalThroughputController.get())
          case x if x.contains(AccountancyServices)  => Redirect(routes.CustomersOutsideIsUKController.get())
          case x if x.contains(HighValueDealing)     => Redirect(routes.CustomersOutsideIsUKController.get())
          case _                                     => Redirect(routes.SummaryController.get)
        }
      case _                => InternalServerError("Unable to redirect from AMP Turnover page")
    }
}
