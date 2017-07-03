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

import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.responsiblepeople.ResponsiblePeople
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import cats.implicits._

class NewOfficerController @Inject()(val authConnector: AuthConnector, cacheConnector: DataCacheConnector) extends BaseController {
  def get = Authorised.async {
    implicit authContext => implicit request =>
      val result = for {
        people <- OptionT(cacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
      } yield {
        Ok(views.html.changeofficer.new_nominated_officer(EmptyForm, people))
      }

      result getOrElse InternalServerError("Could not get the list of responsible people")
  }
}
