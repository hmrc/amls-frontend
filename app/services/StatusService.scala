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

package services

import connectors.AmlsConnector
import models.ReadStatusResponse
import models.registrationprogress.{Completed, Section}
import models.status._
import org.joda.time.LocalDate
import play.api.{Mode, Play}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

trait StatusService {

  private[services] def amlsConnector: AmlsConnector

  private[services] def progressService: ProgressService = Play.current.injector.instanceOf[ProgressService]

  private[services] def enrolmentsService: AuthEnrolmentsService

  private val renewalPeriod = 30

  val Pending = "Pending"
  val Approved = "Approved"
  val Rejected = "Rejected"
  val Revoked = "Revoked"
  val Expired = "Expired"
  val Withdrawal = "Withdrawal"
  val DeRegistered = "De-Registered"


  private def getApprovedStatus(response: ReadStatusResponse) = {
    (response.currentRegYearEndDate, response.renewalConFlag) match {
      case (Some(endDate), false) if LocalDate.now().isAfter(endDate.minusDays(renewalPeriod)) =>
        ReadyForRenewal(response.currentRegYearEndDate)
      case (_, true) => RenewalSubmitted(response.currentRegYearEndDate)
      case _ => SubmissionDecisionApproved
    }
  }

  private def getETMPStatus(response: ReadStatusResponse) = {
    Logger.debug("StatusService:getETMPStatus:formBundleStatus:" + response.formBundleStatus)
    response.formBundleStatus match {
      case `Pending` => SubmissionReadyForReview
      case `Approved` => getApprovedStatus(response)
      case `Rejected` => SubmissionDecisionRejected
      case `Revoked` => SubmissionDecisionRevoked
      case `Expired` => SubmissionDecisionExpired
      case `Withdrawal` => SubmissionWithdrawn
      case `DeRegistered` => models.status.DeRegistered
      case _ if Play.current.mode == Mode.Dev => models.status.NotCompleted
      case _ => throw new RuntimeException("ETMP returned status is inconsistent")
    }
  }

  private def etmpStatusInformation(mlrRegNumber: String)(implicit hc: HeaderCarrier,
                                                          auth: AuthContext, ec: ExecutionContext): Future[(SubmissionStatus, Option[ReadStatusResponse])] = {
    amlsConnector.status(mlrRegNumber) map {
      response =>
        val status = getETMPStatus(response)
        Logger.debug("StatusService:etmpStatusInformation:status:" + status)
        (status, Some(response))
    }
  }

  def getDetailedStatus(implicit hc: HeaderCarrier, authContext: AuthContext, ec: ExecutionContext): Future[(SubmissionStatus, Option[ReadStatusResponse])] = {
    enrolmentsService.amlsRegistrationNumber flatMap {
      case Some(mlrRegNumber) =>
        Logger.debug("StatusService:getDetailedStatus:mlrRegNumber:" + mlrRegNumber)
        etmpStatusInformation(mlrRegNumber)(hc, authContext, ec)
      case None =>
        Logger.debug("StatusService:getDetailedStatus: No mlrRegNumber")
        notYetSubmitted(hc, authContext, ec) map { status =>
          (status, None)
        }
    }
  }

  def getStatus(implicit hc: HeaderCarrier, authContext: AuthContext, ec: ExecutionContext): Future[SubmissionStatus] = {
    enrolmentsService.amlsRegistrationNumber flatMap {
      case Some(mlrRegNumber) =>
        Logger.debug("StatusService:getStatus:mlrRegNumber:" + mlrRegNumber)
        etmpStatus(mlrRegNumber)(hc, authContext, ec)
      case None =>
        Logger.debug("StatusService:getStatus: No mlrRegNumber")
        notYetSubmitted(hc, authContext, ec)
    }
  }

  def getStatus(mlrRegNumber: String)(implicit hc: HeaderCarrier, authContext: AuthContext, ec: ExecutionContext): Future[SubmissionStatus] = {
        etmpStatus(mlrRegNumber)(hc, authContext, ec)
  }

  def getDetailedStatus(mlrRegNumber: String)
                       (implicit hc: HeaderCarrier, auth: AuthContext, ec: ExecutionContext) = etmpStatusInformation(mlrRegNumber)

  def getReadStatus(implicit hc: HeaderCarrier, authContext: AuthContext, ec: ExecutionContext): Future[ReadStatusResponse] = {
    enrolmentsService.amlsRegistrationNumber flatMap {
      case Some(mlrRegNumber) =>
        Logger.debug("StatusService:getReadStatus:mlrRegNumber:" + mlrRegNumber)
        etmpReadStatus(mlrRegNumber)(hc, authContext, ec)
      case _ => throw new RuntimeException("ETMP returned no read status")
    }
  }

  def getReadStatus(mlrRefNo: String)
                   (implicit hc: HeaderCarrier, auth: AuthContext, ec: ExecutionContext) = etmpReadStatus(mlrRefNo)

  private def notYetSubmitted(implicit hc: HeaderCarrier, auth: AuthContext, ec: ExecutionContext) = {

    def isComplete(seq: Seq[Section]): Boolean =
      seq forall {
        _.status == Completed
      }

    progressService.sections map {
      sections =>
        if (isComplete(sections)) {
          Logger.debug("StatusService:notYetSubmitted: SubmissionReady")
          SubmissionReady
        } else {
          Logger.debug("StatusService:notYetSubmitted: NotCompleted")
          NotCompleted
        }
    }
  }

  private def etmpStatus(amlsRefNumber: String)(implicit hc: HeaderCarrier, auth: AuthContext, ec: ExecutionContext): Future[SubmissionStatus] = {
    {
      amlsConnector.status(amlsRefNumber) map {
        response => getETMPStatus(response)
      }
    }
  }

  private def etmpReadStatus(amlsRefNumber: String)(implicit hc: HeaderCarrier, auth: AuthContext, ec: ExecutionContext): Future[ReadStatusResponse] = {
    {
      val status = amlsConnector.status(amlsRefNumber)
      Logger.debug("StatusService:etmpReadStatus:status:" + status)
      status
    }
  }

  def isPending(status: SubmissionStatus) = status match {
    case SubmissionReadyForReview | RenewalSubmitted(_) => true
    case _ => false
  }

  def isPreSubmission(implicit hc: HeaderCarrier, auth: AuthContext, ec: ExecutionContext): Future[Boolean] = getStatus map { s => isPreSubmission(s) }

  def isPreSubmission(status: SubmissionStatus) = Set(NotCompleted, SubmissionReady).contains(status)
}

object StatusService extends StatusService {
  override private[services] val amlsConnector: AmlsConnector = AmlsConnector
  override private[services] lazy val enrolmentsService = Play.current.injector.instanceOf[AuthEnrolmentsService]
}
