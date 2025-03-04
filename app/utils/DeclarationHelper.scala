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

package utils

import cats.data.OptionT
import cats.implicits._
import controllers.declaration
import models.registrationprogress.{Completed, TaskRow, Updated}
import models.responsiblepeople.{Partner, ResponsiblePerson}
import models.status._
import play.api.Logging

import java.time.LocalDate
import play.api.i18n.Messages
import play.api.mvc.Call
import services.{RenewalService, SectionsProvider, StatusService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object DeclarationHelper extends Logging {

  def currentPartnersNames(responsiblePeople: Seq[ResponsiblePerson]): Seq[String] =
    nonDeletedResponsiblePeopleWithPositions(responsiblePeople).collect {
      case p if p.positions.get.positions.contains(Partner) => p.personName.get.fullName
    }

  def numberOfPartners(responsiblePeople: Seq[ResponsiblePerson]): Int =
    nonDeletedResponsiblePeopleWithPositions(responsiblePeople).collect {
      case p if p.positions.get.positions.contains(Partner) => p
    }.size

  def nonPartners(responsiblePeople: Seq[ResponsiblePerson]): Seq[ResponsiblePerson] =
    nonDeletedResponsiblePeopleWithPositions(responsiblePeople).collect {
      case p if !p.positions.get.positions.contains(Partner) => p
    }

  private def nonDeletedResponsiblePeopleWithPositions(
    responsiblePeople: Seq[ResponsiblePerson]
  ): Seq[ResponsiblePerson] =
    responsiblePeople.collect {
      case p if p.positions.isDefined & p.status.isEmpty => p
    }

  def routeDependingOnNominatedOfficer(hasNominatedOfficer: Boolean, status: SubmissionStatus): Call =
    if (hasNominatedOfficer) {
      declaration.routes.WhoIsRegisteringController.get
    } else {
      routeWithoutNominatedOfficer(status)
    }

  private def routeWithoutNominatedOfficer(status: SubmissionStatus): Call =
    status match {
      case SubmissionReady | NotCompleted | SubmissionReadyForReview | ReadyForRenewal(_) =>
        declaration.routes.WhoIsTheBusinessNominatedOfficerController.get
      case _                                                                              => declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment()
    }

  def statusSubtitle(amlsRegistrationNo: Option[String], accountTypeId: (String, String), cacheId: String)(implicit
    statusService: StatusService,
    hc: HeaderCarrier,
    ec: ExecutionContext,
    messages: Messages
  ): Future[String] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReady                                       => "submit.registration"
      case SubmissionReadyForReview | SubmissionDecisionApproved => "submit.amendment.application"
      case ReadyForRenewal(_) | RenewalSubmitted(_)              => "submit.renewal.application"
      case _                                                     => throw new Exception("Incorrect status - Page not permitted for this status")
    }

  def statusEndDate(amlsRegistrationNo: Option[String], accountTypeId: (String, String), cacheId: String)(implicit
    statusService: StatusService,
    hc: HeaderCarrier,
    ec: ExecutionContext,
    messages: Messages
  ): Future[Option[LocalDate]] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case ReadyForRenewal(endDate) => endDate
      case _                        => None
    }

  def promptRenewal(amlsRegistrationNo: Option[String], accountTypeId: (String, String), cacheId: String)(implicit
    statusService: StatusService,
    renewalService: RenewalService,
    hc: HeaderCarrier,
    ec: ExecutionContext,
    messages: Messages
  ): Future[Boolean] =
    for {
      status   <- statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId)
      inWindow <- inRenewalWindow(status)
      complete <- renewalComplete(renewalService, cacheId)
    } yield (inWindow, complete) match {
      case (true, false) => true
      case _             => false
    }

  private def inRenewalWindow(status: SubmissionStatus): Future[Boolean] =
    status match {
      case ReadyForRenewal(_) => Future.successful(true)
      case _                  => Future.successful(false)
    }

  def renewalComplete(renewalService: RenewalService, credId: String)(implicit ec: ExecutionContext): Future[Boolean] =
    renewalService.getRenewal(credId) flatMap {
      case Some(renewal) =>
        renewalService.isRenewalComplete(renewal, credId)
      case _             => Future.successful(false)
    }

  def sectionsComplete(cacheId: String, sectionsProvider: SectionsProvider, isRenewal: Boolean)(implicit
    ec: ExecutionContext,
    messages: Messages
  ): Future[Boolean] = {
    val taskRows: Future[Seq[TaskRow]] =
      if (isRenewal) sectionsProvider.taskRowsForRenewal(cacheId) else sectionsProvider.taskRows(cacheId)
    taskRows map areComplete
  }

  private def areComplete(taskRows: Seq[TaskRow]): Boolean = taskRows.forall { row =>
    row.status == Completed || row.status == Updated
  }

  def getSubheadingBasedOnStatus(
    credId: String,
    amlsRefNumber: Option[String],
    accountTypeId: (String, String),
    statusService: StatusService,
    renewalService: RenewalService
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): OptionT[Future, String] =
    for {
      renewalComplete <- OptionT.liftF(DeclarationHelper.renewalComplete(renewalService, credId))
      status          <- OptionT.liftF(statusService.getStatus(amlsRefNumber, accountTypeId, credId))
    } yield status match {
      case ReadyForRenewal(_) if renewalComplete => "submit.renewal.application"
      case SubmissionReady                       => "submit.registration"
      case _                                     => "submit.amendment.application"
    }
}
