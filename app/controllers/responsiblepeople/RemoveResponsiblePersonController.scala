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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ResponsiblePerson, ResponsiblePersonEndDate}
import models.status._
import services.StatusService
import utils.{RepeatingSection, StatusConstants}
import views.html.responsiblepeople.remove_responsible_person

import scala.concurrent.Future

trait RemoveResponsiblePersonController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private[controllers] def statusService: StatusService

  private def showRemovalDateField(status: SubmissionStatus, lineIdExists: Boolean): Boolean = {
    status match {
      case SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_) if lineIdExists => true
      case _ => false
    }
  }

  def get(index: Int, complete: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        rp <- getData[ResponsiblePerson](index)
        status <- statusService.getStatus
      } yield rp match {
        case (Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))) =>
          Ok(views.html.responsiblepeople.remove_responsible_person(
            EmptyForm, index, personName.fullName, complete, showRemovalDateField(status, rp.get.lineId.isDefined), flow
          ))
        case _ => NotFound(notFoundView)
      }
  }

  def remove(index: Int, complete: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>

        def redirectAppropriately = complete match {
          case true => Redirect(routes.YourAnswersController.get())
          case false => Redirect(routes.DetailedAnswersController.get(index, flow))
        }

        def removeWithoutDate = removeDataStrict[ResponsiblePerson](index) map { _ =>
          redirectAppropriately
        }

        statusService.getStatus flatMap {
          case NotCompleted | SubmissionReady => removeWithoutDate
          case SubmissionReadyForReview => for {
            _ <- updateDataStrict[ResponsiblePerson](index)(_.copy(status = Some(StatusConstants.Deleted), hasChanged = true))
          } yield redirectAppropriately
          case _ =>
            getData[ResponsiblePerson](index) flatMap { _ match {
                case Some(person) if person.lineId.isEmpty => removeWithoutDate
                case Some(person) =>
                  val name = person.personName.fold("")(_.fullName)

                  val extraFields = Map(
                    "positionStartDate" -> Seq(person.positions.get.startDate.get.toString("yyyy-MM-dd")),
                    "userName" -> Seq(name)
                  )

                  Form2[ResponsiblePersonEndDate](request.body.asFormUrlEncoded.get ++ extraFields) match {
                    case f: InvalidForm =>
                      Future.successful(BadRequest(remove_responsible_person(f, index, name, complete, true, flow)))

                    case ValidForm(_, data) => {
                      for {
                        _ <- updateDataStrict[ResponsiblePerson](index) {
                          _.copy(status = Some(StatusConstants.Deleted), endDate = Some(data), hasChanged = true)
                        }
                      } yield redirectAppropriately
                    }
                    case _ => removeWithoutDate
                  }
              }
            }
        }
  }
}

object RemoveResponsiblePersonController extends RemoveResponsiblePersonController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override private[controllers] val statusService: StatusService = StatusService
}
