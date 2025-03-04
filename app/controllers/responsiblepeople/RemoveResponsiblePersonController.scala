/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.responsiblepeople.RemoveResponsiblePersonFormProvider
import models.responsiblepeople.ResponsiblePerson
import models.status._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.StatusService
import utils.{AuthAction, RepeatingSection, StatusConstants}
import views.html.responsiblepeople.RemoveResponsiblePersonView

import java.time.format.DateTimeFormatter.ofPattern
import scala.concurrent.Future

class RemoveResponsiblePersonController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val cc: MessagesControllerComponents,
  formProvider: RemoveResponsiblePersonFormProvider,
  view: RemoveResponsiblePersonView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, flow: Option[String] = None): Action[AnyContent] = authAction.async { implicit request =>
    for {
      rp     <- getData[ResponsiblePerson](request.credId, index)
      status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
    } yield rp match {
      case Some(person) if person.lineId.isDefined && !person.isComplete =>
        Redirect(routes.WhatYouNeedController.get(index, flow))
      case Some(person) if person.personName.isDefined                   =>
        Ok(
          view(
            formProvider(),
            index = index,
            personName = person.personName.get.fullName,
            showDateField = showRemovalDateField(status, rp.get.lineId.isDefined),
            flow = flow
          )
        )
      case _                                                             => NotFound(notFoundView)
    }
  }

  def remove(index: Int, flow: Option[String] = None): Action[AnyContent] = authAction.async { implicit request =>
    def removeWithoutDate(): Future[Result] = removeDataStrict[ResponsiblePerson](request.credId, index) map { _ =>
      Redirect(routes.YourResponsiblePeopleController.get())
    }

    statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap { status =>
      getData[ResponsiblePerson](request.credId, index) flatMap { personData =>
        (status, personData) match {
          case (NotCompleted, _)                                                   => removeWithoutDate()
          case (SubmissionReady, _)                                                => removeWithoutDate()
          case (SubmissionReadyForReview, None)                                    => removeWithoutDate()
          case (SubmissionReadyForReview, Some(person)) if person.lineId.isDefined =>
            updateDataStrict[ResponsiblePerson](request.credId, index)(
              _.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
            ).map { _ =>
              Redirect(routes.YourResponsiblePeopleController.get())
            }
          case (_, Some(person)) if person.lineId.isEmpty                          => removeWithoutDate()
          case (_, Some(person))                                                   =>
            val name = person.personName.fold("")(_.titleName)
            formProvider()
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(formWithErrors, index, name, showDateField = true, flow))),
                data => {
                  val startDate = person.positions.flatMap(_.startDate.map(_.startDate))
                  data match {
                    case Left(_)                                                    => removeWithoutDate()
                    case Right(value) if startDate.exists(_.isAfter(value.endDate)) =>
                      val formWithFutureError =
                        formProvider().withError(
                          "endDate",
                          messages(
                            "error.expected.rp.date.after.start",
                            name,
                            startDate.fold("")(strtDte => strtDte.format(ofPattern("dd-MM-yyyy")))
                          )
                        )
                      Future.successful(BadRequest(view(formWithFutureError, index, name, showDateField = true, flow)))
                    case Right(value)                                               =>
                      updateDataStrict[ResponsiblePerson](request.credId, index) {
                        _.copy(status = Some(StatusConstants.Deleted), endDate = Some(value), hasChanged = true)
                      }.map(_ => Redirect(routes.YourResponsiblePeopleController.get()))
                  }
                }
              )
        }
      }
    }
  }

  private def showRemovalDateField(status: SubmissionStatus, lineIdExists: Boolean): Boolean =
    status match {
      case SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_) if lineIdExists => true
      case _                                                                                     => false
    }
}
