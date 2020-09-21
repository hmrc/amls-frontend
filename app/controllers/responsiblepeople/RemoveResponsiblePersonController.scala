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

package controllers.responsiblepeople

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ResponsiblePerson, ResponsiblePersonEndDate}
import models.status._
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import utils.{AuthAction, RepeatingSection, StatusConstants}
import views.html.responsiblepeople.remove_responsible_person

import scala.concurrent.Future


class RemoveResponsiblePersonController @Inject () (
                                                   val dataCacheConnector: DataCacheConnector,
                                                   authAction: AuthAction,
                                                   val ds: CommonPlayDependencies,
                                                   val statusService: StatusService,
                                                   val cc: MessagesControllerComponents,
                                                   remove_responsible_person: remove_responsible_person,
                                                   implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, flow: Option[String] = None) = authAction.async {
    implicit request =>
      for {
        rp <- getData[ResponsiblePerson](request.credId, index)
        status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
      } yield rp match {
        case Some(person) if (person.lineId.isDefined && !person.isComplete) =>
          Redirect(routes.WhatYouNeedController.get(index, flow))
        case (Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))) =>
          Ok(remove_responsible_person(
            f = EmptyForm,
            index = index,
            personName = personName.fullName,
            showDateField = showRemovalDateField(status, rp.get.lineId.isDefined),
            flow = flow
          ))
        case _ => NotFound(notFoundView)
      }
  }

  def remove(index: Int, flow: Option[String] = None) = authAction.async {
    implicit request =>
        def removeWithoutDate = removeDataStrict[ResponsiblePerson](request.credId, index) map { _ =>
          Redirect(routes.YourResponsiblePeopleController.get())
        }

        statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
          case NotCompleted | SubmissionReady => removeWithoutDate
          case SubmissionReadyForReview =>
              getData[ResponsiblePerson](request.credId, index) flatMap {
                  case Some(person) if person.lineId.isDefined => for {
                      _ <- updateDataStrict[ResponsiblePerson](request.credId, index)(_.copy(status = Some(StatusConstants.Deleted), hasChanged = true))
                  } yield Redirect(routes.YourResponsiblePeopleController.get())
                  case _ => removeWithoutDate
              }
          case _ =>
            getData[ResponsiblePerson](request.credId, index) flatMap { _ match {
                case Some(person) if person.lineId.isEmpty => removeWithoutDate
                case Some(person) =>
                  val name = person.personName.fold("")(_.fullName)
                  val startDate = person.positions
                    .flatMap(p => p.startDate)
                    .map(d => Seq(d.startDate.toString("yyyy-MM-dd")))
                    .getOrElse(Seq())
                  val extraFields = Map(
                    "positionStartDate" -> startDate,
                    "userName" -> Seq(name)
                  )

                  Form2[ResponsiblePersonEndDate](request.body.asFormUrlEncoded.get ++ extraFields) match {
                    case f: InvalidForm =>
                      Future.successful(BadRequest(remove_responsible_person(f, index, name, true, flow)))

                    case ValidForm(_, data) => {
                      for {
                        _ <- updateDataStrict[ResponsiblePerson](request.credId, index) {
                          _.copy(status = Some(StatusConstants.Deleted), endDate = Some(data), hasChanged = true)
                        }
                      } yield Redirect(routes.YourResponsiblePeopleController.get())
                    }
                    case _ => removeWithoutDate
                  }
              }
            }
        }
  }

  private def showRemovalDateField(status: SubmissionStatus, lineIdExists: Boolean): Boolean = {
    status match {
      case SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_) if lineIdExists => true
      case _ => false
    }
  }
}