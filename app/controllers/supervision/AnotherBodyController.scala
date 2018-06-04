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

package controllers.supervision

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.supervision.{AnotherBody, Supervision}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.supervision.another_body

import scala.concurrent.Future

class AnotherBodyController @Inject() (val dataCacheConnector: DataCacheConnector,
                                       val authConnector: AuthConnector
                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Supervision](Supervision.key) map {
        response =>
          val form: Form2[AnotherBody] = (for {
            supervision <- response
            anotherBody <- supervision.anotherBody
          } yield Form2[AnotherBody](anotherBody)).getOrElse(EmptyForm)
          Ok(another_body(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[AnotherBody](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(another_body(f, edit)))
        case ValidForm(_, data) =>
          for {
            supervision <- dataCacheConnector.fetch[Supervision](Supervision.key)
            _ <- dataCacheConnector.save[Supervision](Supervision.key,
              supervision.anotherBody(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.ProfessionalBodyMemberController.get())
          }
      }
  }
}
