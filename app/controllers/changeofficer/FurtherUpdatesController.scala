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
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.changeofficer.Helpers._
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.changeofficer._
import models.responsiblepeople.{NominatedOfficer, Positions, ResponsiblePeople}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

class FurtherUpdatesController @Inject()(
                                          val authConnector: AuthConnector,
                                          implicit val dataCacheConnector: DataCacheConnector
                                        ) extends BaseController with RepeatingSection {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Ok(views.html.changeofficer.further_updates(EmptyForm)))
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request => Form2[FurtherUpdates](request.body) match {
        case ValidForm(_, data) => {
          (for {
            cache <- OptionT(dataCacheConnector.fetchAll)
            responsiblePeople <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key))
            changeOfficer <- OptionT.fromOption[Future](cache.getEntry[ChangeOfficer](ChangeOfficer.key))
            (_, iNew) <- OptionT.fromOption[Future](getNominatedOfficer(changeOfficer.newOfficer, responsiblePeople))
            (_, iOld) <- OptionT.fromOption[Future](getNominatedOfficer(changeOfficer.oldOfficer, responsiblePeople))
            _ <- OptionT.liftF(dataCacheConnector.save[Seq[ResponsiblePeople]](ResponsiblePeople.key, {
              updateNominatedOfficers(responsiblePeople, iNew, iOld)
            }))
          } yield {
            Redirect(
              data match {
                case FurtherUpdatesYes => controllers.routes.RegistrationProgressController.get()
                case FurtherUpdatesNo => controllers.declaration.routes.WhoIsRegisteringController.get()
              })
          }) getOrElse InternalServerError("Cannot save new Nominated Officer")
        }
        case f: InvalidForm => Future.successful(BadRequest(views.html.changeofficer.further_updates(f)))
      }
  }

  private def updateNominatedOfficers(responsiblePeople: Seq[ResponsiblePeople], iNew: Int, iOld: Int) = {
    removeNominatedOfficers(responsiblePeople).patch(iNew, Seq(addNominatedOfficer(responsiblePeople(iNew))), 1)
  }

  private def getNominatedOfficer(officer: Option[Officer], responsiblePeople: Seq[ResponsiblePeople]) = {
    officer match {
      case Some(_) => officer flatMap { o =>
        matchOfficerWithResponsiblePerson(o, responsiblePeople)
      }
      case _ => getOfficer(responsiblePeople)
    }
  }

  private def addNominatedOfficer(responsiblePerson: ResponsiblePeople): ResponsiblePeople = {
    val positions = responsiblePerson.positions.get
    responsiblePerson.positions(
      Positions(positions.positions + NominatedOfficer, positions.startDate)
    )
  }

  private def removeNominatedOfficers(responsiblePeople: Seq[ResponsiblePeople]): Seq[ResponsiblePeople] = {

    responsiblePeople map { responsiblePerson =>
      val positions = responsiblePerson.positions.get
      responsiblePerson.positions(
        Positions(positions.positions - NominatedOfficer, positions.startDate)
      )
    }

  }

}
