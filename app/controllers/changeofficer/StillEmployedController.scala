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

package controllers.changeofficer

import javax.inject.Inject

import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.changeofficer.Helpers._
import forms.{EmptyForm, Form2, InvalidForm}
import models.changeofficer.StillEmployed
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class StillEmployedController @Inject()
(val authConnector: AuthConnector, implicit val dataCacheConnector: DataCacheConnector) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>

      (getNominatedOfficerName map (name =>
        Ok(views.html.changeofficer.still_employed(EmptyForm, name))
        )) getOrElse InternalServerError("No responsible people found")
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      Form2[StillEmployed](request.body) match {
        case x: InvalidForm =>
          (getNominatedOfficerName map (name =>
            BadRequest(views.html.changeofficer.still_employed(x, name))
            )) getOrElse InternalServerError("No responsible people found")
        case _ => Future.successful(Redirect(controllers.changeofficer.routes.RoleInBusinessController.get()))
      }
  }
}
