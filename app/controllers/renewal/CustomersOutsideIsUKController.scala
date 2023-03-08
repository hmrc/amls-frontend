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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}

import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivity, BusinessMatching}
import models.businessmatching.BusinessActivity.HighValueDealing
import models.renewal.{CustomersOutsideIsUK, Renewal}
import play.api.mvc.MessagesControllerComponents
import services.{AutoCompleteService, RenewalService}
import utils.AuthAction
import views.html.renewal._

import scala.concurrent.Future

@Singleton
class CustomersOutsideIsUKController @Inject()(val dataCacheConnector: DataCacheConnector,
                                               val authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val renewalService: RenewalService,
                                               val autoCompleteService: AutoCompleteService,
                                               val cc: MessagesControllerComponents,
                                               customers_outside_uk_isUK: customers_outside_uk_isUK) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        renewalService.getRenewal(request.credId) map {
          response =>
            val form: Form2[CustomersOutsideIsUK] = (for {
              renewal <- response
              customers <- renewal.customersOutsideIsUK
            } yield Form2[CustomersOutsideIsUK](customers)).getOrElse(EmptyForm)
            Ok(customers_outside_uk_isUK(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[CustomersOutsideIsUK](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(customers_outside_uk_isUK(f, edit)))
          case ValidForm(_, data: CustomersOutsideIsUK) =>
            dataCacheConnector.fetchAll(request.credId) flatMap { optionalCache =>
              (for {
                cache <- optionalCache
                renewal <- cache.getEntry[Renewal](Renewal.key)
                businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                ba <- businessMatching.activities.map { a => a.businessActivities}
              } yield {
                renewalService.updateRenewal(request.credId, data.isOutside match {
                  case false => renewal.customersOutsideIsUK(data).copy(customersOutsideUK = None)
                  case true => renewal.customersOutsideIsUK(data)
                }) map { _ =>
                  redirectTo(data, edit, ba)
                }
              }) getOrElse Future.successful(InternalServerError("Unable to get data from the cache"))
            }
        }
  }
  def redirectTo(outsideUK: CustomersOutsideIsUK, edit: Boolean, ba: Set[BusinessActivity]) = {
    (outsideUK, edit) match {
      case (CustomersOutsideIsUK(true), false) => Redirect(routes.CustomersOutsideUKController.get())
      case (CustomersOutsideIsUK(true), true) => Redirect(routes.CustomersOutsideUKController.get(true))
      case (CustomersOutsideIsUK(false), _) => (ba, edit) match {
        case (x, false) if x.contains(HighValueDealing) => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
        case _ => Redirect(routes.SummaryController.get)
      }
    }
  }
}


