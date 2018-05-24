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

import javax.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessmatching.{AccountancyServices, BusinessMatching, HighValueDealing, MoneyServiceBusiness}
import models.renewal.{AMLSTurnover, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.amls_turnover

@Singleton
class AMLSTurnoverController @Inject()(
                                        val dataCacheConnector: DataCacheConnector,
                                        val authConnector: AuthConnector,
                                        val renewalService: RenewalService
                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        dataCacheConnector.fetchAll map {
          optionalCache =>
            (for {
              cache <- optionalCache
              businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            } yield {

              val form = (for {
                renewal <- cache.getEntry[Renewal](Renewal.key)
                turnover <- renewal.turnover
              } yield Form2[AMLSTurnover](turnover)) getOrElse EmptyForm

              Ok(amls_turnover(form, edit, businessMatching.activities))

            }) getOrElse Ok(amls_turnover(EmptyForm, edit, None))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
        Form2[AMLSTurnover](request.body) match {
          case f: InvalidForm =>
            for {
              businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            } yield BadRequest(amls_turnover(f, edit, businessMatching.activities))
          case ValidForm(_, data) =>
            for {
              renewal <- renewalService.getRenewal
              _ <- renewalService.updateRenewal(renewal.turnover(data))
              businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            } yield (edit, businessMatching) match {
              case (true, _) => Redirect(routes.SummaryController.get())
              case (false, Some(bm)) if bm.activities.isDefined => bm.activities.get.businessActivities match {
                case x if x.contains(HighValueDealing) => Redirect(routes.CustomersOutsideUKController.get())
                case x if x.contains(AccountancyServices) => Redirect(routes.CustomersOutsideUKController.get())
                case x if x.contains(MoneyServiceBusiness) => Redirect(routes.TotalThroughputController.get())
                case _ => Redirect(routes.SummaryController.get())
              case _ => InternalServerError("Unable to redirect from Turnover page")
              }
            }
        }
      }
  }

}
