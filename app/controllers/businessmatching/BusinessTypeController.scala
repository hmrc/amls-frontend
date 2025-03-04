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

package controllers.businessmatching

import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.BusinessTypeFormProvider
import models.businessmatching.BusinessType
import models.businessmatching.BusinessType._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.businessmatching.BusinessTypeService
import utils.AuthAction
import views.html.businessmatching._

import javax.inject.Inject
import scala.concurrent.Future

class BusinessTypeController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  service: BusinessTypeService,
  formProvider: BusinessTypeFormProvider,
  view: BusinessTypeView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    service.getBusinessType(request.credId) map { btOpt =>
      btOpt.map(getResultByBusiness).getOrElse(Ok(view(formProvider())))
    }
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        data =>
          service.updateBusinessType(request.credId, data).map {
            _.fold(Redirect(routes.RegisterServicesController.get()))(getResultByBusiness)
          }
      )
  }

  private def getResultByBusiness(businessType: BusinessType): Result = businessType match {
    case UnincorporatedBody      => Redirect(routes.TypeOfBusinessController.get())
    case LPrLLP | LimitedCompany => Redirect(routes.CompanyRegistrationNumberController.get())
    case _                       => Redirect(routes.RegisterServicesController.get())
  }
}
