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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import javax.inject.{Inject, Singleton}
import models.renewal.{PercentageOfCashPaymentOver15000, Renewal}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.percentage

import scala.concurrent.Future


@Singleton
class PercentageOfCashPaymentOver15000Controller @Inject()(
                                           val dataCacheConnector: DataCacheConnector,
                                           val authAction: AuthAction, val ds: CommonPlayDependencies,
                                           val renewalService: RenewalService
                                         ) extends AmlsBaseController(ds) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[Renewal](request.credId, Renewal.key) map {
        response =>
          val form: Form2[PercentageOfCashPaymentOver15000] = (for {
            renewal <- response
            percentageOfCashPaymentOver15000 <- renewal.percentageOfCashPaymentOver15000
          } yield Form2[PercentageOfCashPaymentOver15000](percentageOfCashPaymentOver15000)).getOrElse(EmptyForm)
          Ok(percentage(form, edit))
      }

  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[PercentageOfCashPaymentOver15000](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.renewal.percentage(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- renewalService.getRenewal(request.credId)
            _ <- renewalService.updateRenewal(request.credId, renewal.percentageOfCashPaymentOver15000(data))
          } yield redirectDependingOnEdit(edit)
      }
    }
  }

  private def redirectDependingOnEdit(edit: Boolean) = edit match {
    case true => Redirect(routes.SummaryController.get())
    case false => Redirect(routes.CashPaymentsCustomersNotMetController.get(edit))
  }

}
