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
import forms.{InvalidForm, ValidForm, Form2, EmptyForm}
import models.changeofficer.{NewOfficer, ChangeOfficer}
import models.responsiblepeople.ResponsiblePeople
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import cats.implicits._

class NewOfficerController @Inject()(val authConnector: AuthConnector, cacheConnector: DataCacheConnector) extends BaseController {
  def get = Authorised.async {
    implicit authContext => implicit request =>

      val result = getPeopleAndSelectedOfficer() map { t =>
        Ok(views.html.changeofficer.new_nominated_officer(Form2[NewOfficer](t._1), t._2))
      }

      result getOrElse {
        InternalServerError("Could not get the list of responsible people")
      }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      Form2[NewOfficer](request.body) match {
        case f: InvalidForm =>
          val result = getPeopleAndSelectedOfficer() map { t =>
            BadRequest(views.html.changeofficer.new_nominated_officer(f, t._2))
          }

          result getOrElse {
            InternalServerError("Could not get the list of responsible people")
          }

        case ValidForm(_, data) =>

          val result = for {
            changeOfficer <- OptionT(cacheConnector.fetch[ChangeOfficer](ChangeOfficer.key))
            _ <- OptionT.liftF(cacheConnector.save(ChangeOfficer.key, changeOfficer.copy(newOfficer = Some(data))))
          } yield {
            Redirect(controllers.changeofficer.routes.FurtherUpdatesController.get())
          }

          result getOrElse InternalServerError("No ChangeOfficer Role found")

      }
  }

  private def getPeopleAndSelectedOfficer()(implicit headerCarrier: HeaderCarrier, authContext: AuthContext) = {
    for {
      people <- OptionT(cacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
      changeOfficer <- OptionT(cacheConnector.fetch[ChangeOfficer](ChangeOfficer.key))
      selectedOfficer <- OptionT.fromOption[Future](changeOfficer.newOfficer) orElse OptionT.some(NewOfficer(""))
    } yield {
      (selectedOfficer, people)
    }
  }
}
