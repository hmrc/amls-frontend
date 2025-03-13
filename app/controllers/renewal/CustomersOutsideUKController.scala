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
import forms.renewal.CustomersOutsideUKFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.businessmatching._
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AutoCompleteService, RenewalService}
import utils.AuthAction
import views.html.renewal.CustomersOutsideUKView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CustomersOutsideUKController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  formProvider: CustomersOutsideUKFormProvider,
  view: CustomersOutsideUKView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    renewalService.getRenewal(request.credId).map { response =>
      val form = (for {
        renewal   <- response
        customers <- renewal.customersOutsideUK
      } yield formProvider().fill(customers)).getOrElse(formProvider())
      Ok(view(form, edit, autoCompleteService.formOptions))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, autoCompleteService.formOptions))),
        data =>
          dataCacheConnector.fetchAll(request.credId).flatMap { optionalCache =>
            (for {
              cache            <- optionalCache
              businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
              renewal          <- cache.getEntry[Renewal](Renewal.key)
            } yield renewalService.updateRenewal(request.credId, renewal.customersOutsideUK(data)) map { _ =>
              (edit, businessMatching) match {
                case (true, _)                              => Redirect(routes.SummaryController.get)
                case (false, bm) if bm.activities.isDefined =>
                  bm.activities.get.businessActivities match {
                    case x if x.contains(HighValueDealing) =>
                      Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
                    case _                                 => Redirect(routes.SummaryController.get)
                  }
              }
            }) getOrElse Future.successful(InternalServerError("Unable to get data from the cache"))
          }
      )
  }
}
