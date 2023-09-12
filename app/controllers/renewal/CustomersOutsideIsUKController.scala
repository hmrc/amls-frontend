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
import forms.renewal.CustomersOutsideIsUKFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.businessmatching.{BusinessActivity, BusinessMatching}
import models.renewal.{CustomersOutsideIsUK, Renewal}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AutoCompleteService, RenewalService}
import utils.AuthAction
import views.html.renewal.CustomersOutsideIsUKView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CustomersOutsideIsUKController @Inject()(val dataCacheConnector: DataCacheConnector,
                                               val authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val renewalService: RenewalService,
                                               val autoCompleteService: AutoCompleteService,
                                               val cc: MessagesControllerComponents,
                                               formProvider: CustomersOutsideIsUKFormProvider,
                                               view: CustomersOutsideIsUKView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      renewalService.getRenewal(request.credId) map {
        response =>
          val form = (for {
            renewal <- response
            customers <- renewal.customersOutsideIsUK
          } yield formProvider().fill(customers)).getOrElse(formProvider())
          Ok(view(form, edit))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
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
      )
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


