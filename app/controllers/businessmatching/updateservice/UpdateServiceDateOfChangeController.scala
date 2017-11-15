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
import models.DateOfChange
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import routes._

import scala.concurrent.Future

@Singleton
class UpdateServiceDateOfChangeController @Inject()(
                                                   val authConnector: AuthConnector
                                                   ) extends BaseController {

  def get(services: String) = Authorised.async{
    implicit authContext =>
      implicit request =>
        Future.successful(Ok(view(EmptyForm)))
  }

  def post(services: String) = Authorised.async{
    implicit authContext =>
      implicit request =>
      Form2[DateOfChange](request.body) match {
        case ValidForm(_, _) => Future.successful(Redirect(UpdateAnyInformationController.get()))
        case f:InvalidForm => Future.successful(BadRequest(view(f)))
      }
  }

  private def view(f: Form2[_])(implicit request: Request[_]) =
    views.html.date_of_change(f, "summary.updateservice", UpdateServiceDateOfChangeController.post(""))

}
