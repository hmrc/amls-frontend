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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.renewal.{AMLSTurnover, Renewal}
import play.api.mvc.MessagesControllerComponents
import services.RenewalService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.amls_turnover


import scala.concurrent.Future

@Singleton
class AMLSTurnoverController @Inject()(val dataCacheConnector: DataCacheConnector,
                                       val authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       val renewalService: RenewalService,
                                       val cc: MessagesControllerComponents,
                                       amls_turnover: amls_turnover) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          } yield {
            val form = (for {
              renewal <- cache.getEntry[Renewal](Renewal.key)
              turnover <- renewal.turnover
            } yield Form2[AMLSTurnover](turnover)) getOrElse EmptyForm
            Ok(amls_turnover(form, edit, businessMatching.alphabeticalBusinessActivitiesLowerCase(false)))
          }) getOrElse Ok(amls_turnover(EmptyForm, edit, None))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      getErrorMessage(request.credId) flatMap { errorMsg =>
        Form2[AMLSTurnover](request.body)(AMLSTurnover.formRuleWithErrorMsg(errorMsg)) match {
          case f: InvalidForm =>
            for {
              businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
            } yield BadRequest(amls_turnover(f, edit, businessMatching.alphabeticalBusinessActivitiesLowerCase(false)))
          case ValidForm(_, data) =>
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
        }
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

  private def getErrorMessage(credId: String)(implicit hc: HeaderCarrier) = {
    (for {
      businessMatching <- OptionT(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
      activities <- OptionT.fromOption[Future](businessMatching.activities)
    } yield {
      if (activities.businessActivities.size == 1) {
        "error.required.renewal.ba.turnover.from.mlr.single.service"
      } else {
        "error.required.renewal.ba.turnover.from.mlr"
      }
    }) getOrElse "error.required.renewal.ba.turnover.from.mlr"
  }
}
