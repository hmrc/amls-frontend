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
            oldOfficer <- OptionT.fromOption[Future](getOfficer(responsiblePeople))
            newOfficer <- OptionT.fromOption[Future](changeOfficer.newOfficer)
            (_, index) <- OptionT.fromOption[Future](ResponsiblePeople.findResponsiblePersonByName(newOfficer.name, responsiblePeople))
            _ <- OptionT.liftF(dataCacheConnector.save[Seq[ResponsiblePeople]](ResponsiblePeople.key, {
              updateNominatedOfficers(oldOfficer, changeOfficer.roleInBusiness, responsiblePeople, index)
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

  private def updateNominatedOfficers(oldOfficer: (ResponsiblePeople, Int), roles: RoleInBusiness, responsiblePeople: Seq[ResponsiblePeople], index: Int) = {
    removeNominatedOfficers(responsiblePeople)
      .patch(oldOfficer._2 - 1, Seq(updateRoles(oldOfficer._1, roles)), 1)
      .patch(index, Seq(addNominatedOfficer(responsiblePeople(index))), 1)
      .map(_.copy(hasAccepted = true))
  }

  private def updateRoles(oldOfficer: ResponsiblePeople, rolesInBusiness: RoleInBusiness): ResponsiblePeople = {
    import models.changeofficer.RoleInBusiness._
    val positions = oldOfficer.positions.fold(Positions(Set.empty, None))(p => p)
    oldOfficer.positions(Positions(rolesInBusiness.roles, positions.startDate))
  }

  private def addNominatedOfficer(responsiblePerson: ResponsiblePeople): ResponsiblePeople = {
    val positions = responsiblePerson.positions.fold(Positions(Set.empty, None))(p => p)
    responsiblePerson.positions(
      Positions(positions.positions + NominatedOfficer, positions.startDate)
    )
  }

  private def removeNominatedOfficers(responsiblePeople: Seq[ResponsiblePeople]): Seq[ResponsiblePeople] = {
    responsiblePeople map { responsiblePerson =>
      val positions = responsiblePerson.positions.fold(Positions(Set.empty, None))(p => p)
      responsiblePerson.positions(
        Positions(positions.positions - NominatedOfficer, positions.startDate)
      )
    }
  }

}