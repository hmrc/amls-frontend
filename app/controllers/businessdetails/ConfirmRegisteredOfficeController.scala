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

package controllers.businessdetails

import com.google.inject.Inject
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessdetails.ConfirmRegisteredOfficeFormProvider
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.businessdetails.ConfirmRegisteredOfficeService
import utils.AuthAction
import views.html.businessdetails.ConfirmRegisteredOfficeOrMainPlaceView

class ConfirmRegisteredOfficeController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: ConfirmRegisteredOfficeFormProvider,
  service: ConfirmRegisteredOfficeService,
  view: ConfirmRegisteredOfficeOrMainPlaceView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    service.hasRegisteredAddress(request.credId).flatMap { optHasRegAddress =>
      service.getAddress(request.credId).map { optAddress =>
        (optHasRegAddress, optAddress) match {
          case (Some(false), Some(data)) => Ok(view(formProvider(), data))
          case _                         => Redirect(routes.RegisteredOfficeIsUKController.get(edit))
        }
      }
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          service.getAddress(request.credId).map { optAddress =>
            optAddress.fold(Redirect(routes.RegisteredOfficeIsUKController.get(edit))) { data =>
              BadRequest(view(formWithErrors, data))
            }
          },
        data =>
          service.updateRegisteredOfficeAddress(request.credId, data).map {
            case Some(_) if data.isRegOfficeOrMainPlaceOfBusiness =>
              Redirect(routes.BusinessEmailAddressController.get(edit))
            case _                                                =>
              Redirect(routes.RegisteredOfficeIsUKController.get(edit))
          }
      )
  }
}
