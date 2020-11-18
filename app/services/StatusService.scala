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

package services

import com.google.inject.Inject
import connectors.AmlsConnector
import models.ReadStatusResponse
import models.registrationprogress.{Completed, Section}
import models.status._
import org.joda.time.LocalDate
import play.api.{Logger, Mode, Play}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class StatusService @Inject() (val amlsConnector: AmlsConnector,
                               val enrolmentsService: AuthEnrolmentsService,
                               val sectionsProvider: SectionsProvider){
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
    // $COVERAGE-OFF$
    Logger.debug("StatusService:getETMPStatus:formBundleStatus:" + response.formBundleStatus)
    // $COVERAGE-ON$
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

  def getSafeIdFromReadStatus(mlrRegNumber: String, accountTypeId: (String, String))(implicit hc: HeaderCarrier,  ec: ExecutionContext) = {
    amlsConnector.status(mlrRegNumber, accountTypeId) map {
      response =>
        // $COVERAGE-OFF$
        Logger.debug("StatusService:etmpStatusInformation:response:" + response)
        // $COVERAGE-ON$
        Option(response.safeId.getOrElse(""))
    }
  }

  private def etmpStatusInformation(mlrRegNumber: String, accountTypeId: (String, String))
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(SubmissionStatus, Option[ReadStatusResponse])] = {

    amlsConnector.status(mlrRegNumber, accountTypeId) map {
      response =>
        val status = getETMPStatus(response)
        // $COVERAGE-OFF$
        Logger.debug("StatusService:etmpStatusInformation:status:" + status)
        // $COVERAGE-ON$
        (status, Some(response))
    }
  }

  def getDetailedStatus(amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(SubmissionStatus, Option[ReadStatusResponse])] = {
    amlsRegistrationNumber match {
      case Some(mlrRegNumber) =>
        // $COVERAGE-OFF$
        Logger.debug("StatusService:getDetailedStatus:mlrRegNumber:" + mlrRegNumber)
        // $COVERAGE-ON$
        etmpStatusInformation(mlrRegNumber, accountTypeId)(hc, ec)
      case None =>
        // $COVERAGE-OFF$
        Logger.debug("StatusService:getDetailedStatus: No mlrRegNumber")
        // $COVERAGE-ON$
        notYetSubmitted(cacheId)(ec) map { status =>
          (status, None)
        }
    }
  }

  def getStatus(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubmissionStatus] = {
    amlsRegistrationNo match {
        case Some(mlrRegNumber) =>
          // $COVERAGE-OFF$
          Logger.debug("StatusService:getStatus:mlrRegNumber:" + mlrRegNumber)
          // $COVERAGE-ON$
          etmpStatus(mlrRegNumber, accountTypeId)(hc, ec)
        case None =>
          // $COVERAGE-OFF$
          Logger.debug("StatusService:getStatus: No mlrRegNumber")
          // $COVERAGE-ON$
          notYetSubmitted(credId)(ec)
      }
  }

  def getStatus(mlrRegNumber: String, accountTypeId: (String, String))(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubmissionStatus] = {
    etmpStatus(mlrRegNumber, accountTypeId)(hc, ec)
  }

  def getReadStatus(amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ReadStatusResponse] = {
    amlsRegistrationNumber match {
      case Some(mlrRegNumber) =>
        // $COVERAGE-OFF$
        Logger.debug("StatusService:getReadStatus:mlrRegNumber:" + mlrRegNumber)
        // $COVERAGE-ON$
        etmpReadStatus(mlrRegNumber, accountTypeId)(hc, ec)
      case _ => throw new RuntimeException("ETMP returned no read status")
    }
  }

  def getReadStatus(amlsRegistrationNumber: String, accountTypeId: (String, String))
                   (implicit hc: HeaderCarrier, ec: ExecutionContext) =
    etmpReadStatus(amlsRegistrationNumber, accountTypeId)

  private def notYetSubmitted(cacheId: String)(implicit ec: ExecutionContext) = {

    def isComplete(seq: Seq[Section]): Boolean =
      seq forall {
        _.status == Completed
      }

    sectionsProvider.sections(cacheId) map {
      sections =>
        if (isComplete(sections)) {
          // $COVERAGE-OFF$
          Logger.debug("StatusService:notYetSubmitted: SubmissionReady")
          // $COVERAGE-ON$
          SubmissionReady
        } else {
          // $COVERAGE-OFF$
          Logger.debug("StatusService:notYetSubmitted: NotCompleted")
          // $COVERAGE-ON$
          NotCompleted
        }
    }
  }

  private def etmpStatus(amlsRefNumber: String, accountTypeId: (String, String))
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubmissionStatus] = {
    {
      amlsConnector.status(amlsRefNumber, accountTypeId) map {
        response => getETMPStatus(response)
      }
    }
  }

  private def etmpReadStatus(amlsRefNumber: String, accountTypeId: (String, String))
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ReadStatusResponse] = {
    {
      val status = amlsConnector.status(amlsRefNumber, accountTypeId)
      // $COVERAGE-OFF$
      Logger.debug("StatusService:etmpReadStatus:status:" + status)
      // $COVERAGE-ON$
      status
    }
  }

  def isPending(status: SubmissionStatus) = status match {
    case SubmissionReadyForReview | RenewalSubmitted(_) => true
    case _ => false
  }

  def isPreSubmission(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = getStatus(amlsRegistrationNo, accountTypeId, credId) map { s => isPreSubmission(s) }

  def isPreSubmission(status: SubmissionStatus) = Set(NotCompleted, SubmissionReady).contains(status)
}
