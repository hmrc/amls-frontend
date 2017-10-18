/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.businessmatching.updateservice

import javax.inject.{Inject, Singleton}

import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BooleanFormReadWrite

import scala.concurrent.Future

@Singleton
class UpdateAnyInformationController @Inject()(
                                                val authConnector: AuthConnector,
                                                val statusService: StatusService
                                              ) extends BaseController {

  val NAME = "updateAnyInformation"

  implicit val boolWrite = BooleanFormReadWrite.formWrites(NAME)
  implicit val boolRead = BooleanFormReadWrite.formRule(NAME, "error.updateanyInformation.validationerror")

  def get() = Authorised.async{
    implicit authContext => implicit request =>
      statusService.isPreSubmission map {
        case false => Ok(views.html.update_any_information(EmptyForm, routes.UpdateAnyInformationController.post(), "summary.updateinformation"))
        case true => NotFound(notFoundView)
      }
  }


  def post() = Authorised.async{
    implicit authContext => implicit request =>
      Form2[Boolean](request.body) match {
        case ValidForm(_, data) => data match {
          case true => Future.successful(Redirect(controllers.routes.RegistrationProgressController.get().url))
          case false => Future.successful(Redirect(controllers.declaration.routes.WhoIsRegisteringController.get().url))
        }
        case f:InvalidForm => Future.successful(
          BadRequest(views.html.update_any_information(f, routes.UpdateAnyInformationController.post(), "summary.updateinformation"))
        )
      }
  }


}
