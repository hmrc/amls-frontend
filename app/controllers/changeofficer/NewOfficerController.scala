/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import controllers.changeofficer.Helpers.getOfficer
import forms.{Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.changeofficer.{ChangeOfficer, NewOfficer, RoleInBusiness}
import models.responsiblepeople.ResponsiblePerson.flowChangeOfficer
import models.responsiblepeople.{NominatedOfficer, Positions, ResponsiblePerson}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, RepeatingSection, StatusConstants}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class NewOfficerController @Inject()(authAction: AuthAction,
                                     val ds: CommonPlayDependencies,
                                     val cacheConnector: DataCacheConnector,
                                     val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {

  override def dataCacheConnector: DataCacheConnector = cacheConnector

  def get = authAction.async {
    implicit request =>

      val result = getPeopleAndSelectedOfficer(request.credId) map { t =>
        Ok(views.html.changeofficer.new_nominated_officer(Form2[NewOfficer](t._1), t._2))
      }

      result getOrElse {
        InternalServerError("Could not get the list of responsible people")
      }
  }

  def post = authAction.async {
     implicit request =>
      Form2[NewOfficer](request.body) match {
        case f: InvalidForm =>
          val result = getPeopleAndSelectedOfficer(request.credId) map { t =>
            BadRequest(views.html.changeofficer.new_nominated_officer(f, t._2))
          }

          result getOrElse InternalServerError("Could not get the list of responsible people")

        case ValidForm(_, data) =>

          data match {
            case NewOfficer("someoneElse") =>
              Future.successful(Redirect(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(false, Some(flowChangeOfficer))))
            case _ =>
              val result = for {
                changeOfficer <- OptionT(cacheConnector.fetch[ChangeOfficer](request.credId, ChangeOfficer.key)) orElse OptionT.pure(ChangeOfficer(RoleInBusiness(Set.empty)))
                _ <- OptionT.liftF(cacheConnector.save(request.credId,ChangeOfficer.key, changeOfficer.copy(newOfficer = Some(data))))
                cache <- OptionT(cacheConnector.fetchAll(request.credId))
                responsiblePeople <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key))
                changeOfficer2 <- OptionT.fromOption[Future](cache.getEntry[ChangeOfficer](ChangeOfficer.key))
                oldOfficer <- OptionT.fromOption[Future](getOfficer(responsiblePeople.zipWithIndex))
                newOfficer <- OptionT.fromOption[Future](changeOfficer2.newOfficer)
                (_, index) <- OptionT.fromOption[Future](ResponsiblePerson.findResponsiblePersonByName(newOfficer.name, responsiblePeople))
                _ <- OptionT.liftF(cacheConnector.save[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key, {
                  updateNominatedOfficers(oldOfficer, changeOfficer.roleInBusiness, responsiblePeople, index)
                }))
              } yield {
                deleteOldOfficer(request.credId, oldOfficer._1, oldOfficer._2)

                Redirect(controllers.routes.RegistrationProgressController.get())
              }

              result getOrElse InternalServerError("No ChangeOfficer Role found")
          }
      }
  }

  def getPeopleAndSelectedOfficer(cacheId: String)(implicit headerCarrier: HeaderCarrier) = {
    for {
      people <- OptionT(cacheConnector.fetch[Seq[ResponsiblePerson]](cacheId, ResponsiblePerson.key))
      changeOfficer <- OptionT(cacheConnector.fetch[ChangeOfficer](cacheId, ChangeOfficer.key)) orElse OptionT.pure(ChangeOfficer(RoleInBusiness(Set.empty)))
      selectedOfficer <- OptionT.fromOption[Future](changeOfficer.newOfficer) orElse OptionT.some(NewOfficer(""))
    } yield (selectedOfficer, people.filter(p => p.personName.isDefined & p.isComplete & !p.status.contains(StatusConstants.Deleted)))
  }

  private def updateNominatedOfficers(oldOfficer: (ResponsiblePerson, Int), roles: RoleInBusiness, responsiblePeople: Seq[ResponsiblePerson], index: Int) = {
    removeNominatedOfficers(ResponsiblePerson.filter(responsiblePeople))
      .patch(oldOfficer._2 - 1, Seq(updateRoles(oldOfficer._1, roles)), 1)
      .patch(index, Seq(addNominatedOfficer(responsiblePeople(index))), 1)
      .map(_.copy(hasAccepted = true))
  }

  private def updateRoles(oldOfficer: ResponsiblePerson, rolesInBusiness: RoleInBusiness): ResponsiblePerson = {
    import models.changeofficer.RoleInBusiness._
    val positions = oldOfficer.positions.fold(Positions(Set.empty, None))(p => p)
    oldOfficer.positions(Positions(rolesInBusiness.roles, positions.startDate))
  }

  private def addNominatedOfficer(responsiblePerson: ResponsiblePerson): ResponsiblePerson = {
    val positions = responsiblePerson.positions.fold(Positions(Set.empty, None))(p => p)
    responsiblePerson.positions(
      Positions(positions.positions + NominatedOfficer, positions.startDate)
    )
  }

  private def removeNominatedOfficers(responsiblePeople: Seq[ResponsiblePerson]): Seq[ResponsiblePerson] = {
    responsiblePeople map { responsiblePerson =>
      val positions = responsiblePerson.positions.fold(Positions(Set.empty, None))(p => p)
      responsiblePerson.positions(
        Positions(positions.positions - NominatedOfficer, positions.startDate)
      )
    }
  }

  private def deleteOldOfficer(cacheId: String, rp: ResponsiblePerson, index: Int)(implicit hc: HeaderCarrier) = {
    for {
      maybeUpdatedCache <- if (rp.lineId.isEmpty & rp.endDate.isDefined & rp.status.contains(StatusConstants.Deleted)) (removeDataStrict[ResponsiblePerson](cacheId, index))  else {
        Future.successful(None)
      }
    } yield maybeUpdatedCache
  }
}