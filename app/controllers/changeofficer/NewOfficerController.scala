/*
 * Copyright 2019 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.changeofficer.{ChangeOfficer, NewOfficer, RoleInBusiness}
import models.responsiblepeople.ResponsiblePerson
import models.responsiblepeople.ResponsiblePerson.flowChangeOfficer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.StatusConstants

import scala.concurrent.Future

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

          result getOrElse InternalServerError("Could not get the list of responsible people")

        case ValidForm(_, data) =>

          data match {
            case NewOfficer("someoneElse") =>
              Future.successful(Redirect(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(false, Some(flowChangeOfficer))))
            case _ =>
              val result = for {
                changeOfficer <- OptionT(cacheConnector.fetch[ChangeOfficer](ChangeOfficer.key)) orElse OptionT.pure(ChangeOfficer(RoleInBusiness(Set.empty)))
                _ <- OptionT.liftF(cacheConnector.save(ChangeOfficer.key, changeOfficer.copy(newOfficer = Some(data))))
              } yield {
                Redirect(controllers.changeofficer.routes.FurtherUpdatesController.get())
              }

              result getOrElse InternalServerError("No ChangeOfficer Role found")
          }
      }
  }

  def getPeopleAndSelectedOfficer()(implicit headerCarrier: HeaderCarrier, authContext: AuthContext): OptionT[Future, (NewOfficer, Seq[ResponsiblePerson])] = {
    for {
      people <- OptionT(cacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key))
      changeOfficer <- OptionT(cacheConnector.fetch[ChangeOfficer](ChangeOfficer.key)) orElse OptionT.pure(ChangeOfficer(RoleInBusiness(Set.empty)))
      selectedOfficer <- OptionT.fromOption[Future](changeOfficer.newOfficer) orElse OptionT.some(NewOfficer(""))
    } yield (selectedOfficer, people.filter(p => p.personName.isDefined & p.isComplete & !p.status.contains(StatusConstants.Deleted)))
  }
}