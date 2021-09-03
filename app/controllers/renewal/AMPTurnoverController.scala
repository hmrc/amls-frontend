/*
 * Copyright 2021 HM Revenue & Customs
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
import forms._
import javax.inject.{Inject, Singleton}
import models.businessmatching.{AccountancyServices, BusinessActivities, BusinessMatching, HighValueDealing, MoneyServiceBusiness}
import models.renewal.{AMPTurnover, Renewal}
import play.api.mvc.MessagesControllerComponents
import services.RenewalService
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.amp_turnover

import scala.concurrent.Future

@Singleton
class AMPTurnoverController @Inject()(val dataCacheConnector: DataCacheConnector,
                                      val authAction: AuthAction,
                                      val ds: CommonPlayDependencies,
                                      val renewalService: RenewalService,
                                      val cc: MessagesControllerComponents,
                                      amp_turnover: amp_turnover) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) map {
        optionalCache =>
          (for {
            cache <- optionalCache
          } yield {
            val form = (for {
              renewal <- cache.getEntry[Renewal](Renewal.key)
              ampTurnover <- renewal.ampTurnover
            } yield Form2[AMPTurnover](ampTurnover)) getOrElse EmptyForm
            Ok(amp_turnover(form, edit))

          }) getOrElse Ok(amp_turnover(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[AMPTurnover](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(amp_turnover(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- renewalService.getRenewal(request.credId)
            _ <- renewalService.updateRenewal(request.credId, renewal.ampTurnover(data))
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          } yield {
            if (edit) {
              Redirect(routes.SummaryController.get)
            } else {
              getRouting(ControllerHelper.getBusinessActivity(businessMatching))
            }
          }
      }
    }
  }

  private def getRouting(ba: Option[BusinessActivities]) = {
    ba match {
      case Some(activities) => activities.businessActivities match {
        case x if x.contains(MoneyServiceBusiness) => Redirect(routes.TotalThroughputController.get())
        case x if x.contains(AccountancyServices) => Redirect(routes.CustomersOutsideIsUKController.get())
        case x if x.contains(HighValueDealing) => Redirect(routes.CustomersOutsideIsUKController.get())
        case _ => Redirect(routes.SummaryController.get)
      }
      case _ => InternalServerError("Unable to redirect from AMP Turnover page")
    }
  }
}
