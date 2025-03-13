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

import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.CustomersOutsideIsUKFormProvider
import models.businessmatching.BusinessActivity
import models.businessmatching.BusinessActivity.HighValueDealing
import models.renewal.CustomersOutsideIsUK
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AutoCompleteService, RenewalService}
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.CustomersOutsideIsUKView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CustomersOutsideIsUKController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  formProvider: CustomersOutsideIsUKFormProvider,
  view: CustomersOutsideIsUKView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    renewalService.getRenewal(request.credId) map { response =>
      val form = (for {
        renewal   <- response
        customers <- renewal.customersOutsideIsUK
      } yield formProvider().fill(customers)).getOrElse(formProvider())
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent]                                                       = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          renewalService.fetchAndUpdateRenewal(
            request.credId,
            renewal =>
              if (data.isOutside) {
                renewal.customersOutsideIsUK(data)
              } else {
                renewal.customersOutsideIsUK(data).copy(customersOutsideUK = None)
              }
          ) flatMap {
            case Some(_) =>
              renewalService.getBusinessMatching(request.credId) map { bmOpt =>
                redirectTo(
                  data,
                  edit,
                  ControllerHelper.getBusinessActivity(bmOpt).map(_.businessActivities)
                )
              }
            case None    => Future.successful(InternalServerError("Failed to update cache"))
          }
      )
  }
  private def redirectTo(outsideUK: CustomersOutsideIsUK, edit: Boolean, ba: Option[Set[BusinessActivity]]) =
    (outsideUK, edit) match {
      case (CustomersOutsideIsUK(true), false) => Redirect(routes.CustomersOutsideUKController.get())
      case (CustomersOutsideIsUK(true), true)  => Redirect(routes.CustomersOutsideUKController.get(true))
      case (CustomersOutsideIsUK(false), _)    =>
        (ba, edit) match {
          case (x, false) if x.fold(false)(_.contains(HighValueDealing)) =>
            Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
          case _                                                         => Redirect(routes.SummaryController.get)
        }
    }
}
