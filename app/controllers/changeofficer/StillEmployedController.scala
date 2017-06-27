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

import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.responsiblepeople.{NominatedOfficer, ResponsiblePeople}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class StillEmployedController @Inject()(val authConnector: AuthConnector, dataCacheConnector: DataCacheConnector) extends BaseController {
  def get = Authorised.async {
    implicit authContext => implicit request =>

      dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
        case Some(data) => {

          val nominatedOfficer = data.filter(_.positions.fold(false) (p => p.positions.contains(NominatedOfficer))).head

          val name = nominatedOfficer.personName map (n => n.fullName)
          Ok(views.html.changeofficer.still_employed(EmptyForm, name.getOrElse("")))
        }
        case _ => InternalServerError("No responsible people found")
      }

  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Redirect(routes.RoleInBusinessController.get()))
  }
}
