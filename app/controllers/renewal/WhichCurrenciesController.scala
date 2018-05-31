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

package controllers.renewal

import javax.inject.Inject
import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.renewal.{Renewal, WhichCurrencies}
import play.api.mvc.Result
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.which_currencies

import scala.concurrent.Future

class WhichCurrenciesController @Inject()(val authConnector: AuthConnector,
                                          renewalService: RenewalService,
                                          dataCacheConnector: DataCacheConnector) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      val block = for {
        renewal <- OptionT(renewalService.getRenewal)
        whichCurrencies <- OptionT.fromOption[Future](renewal.whichCurrencies)
      } yield {
        Ok(which_currencies(Form2[WhichCurrencies](whichCurrencies), edit))
      }

      block getOrElse Ok(which_currencies(EmptyForm, edit))

  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[WhichCurrencies](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(which_currencies(f, edit)))
        case ValidForm(_, model) =>
          dataCacheConnector.fetchAll flatMap {
            optMap =>
              val result = for {
                cacheMap <- optMap
                renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                activities <- bm.activities
              } yield {
                renewalService.updateRenewal(renewal.whichCurrencies(model)) map { _ =>
                  standardRouting(activities.businessActivities, edit)
                }

              }
              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      }
  }

  private def standardRouting(businessActivities: Set[BusinessActivity], edit: Boolean): Result =
    (businessActivities, edit) match {
      case (x, false) if x.contains(HighValueDealing) && !x.contains(AccountancyServices) => Redirect(routes.CustomersOutsideUKController.get())
      case (x, false) if x.contains(HighValueDealing) => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
      case _ => Redirect(routes.SummaryController.get())
    }

}
