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

package services

import com.google.inject.Inject
import connectors.{AmlsConnector, DataCacheConnector}
import models.ReadStatusResponse
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, TaskRow, Updated}
import models.status._
import play.api.i18n.Messages
import play.api.{Environment, Logging, Mode}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.ZoneOffset.UTC
import java.time.{Clock, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class StatusService @Inject() (
  val amlsConnector: AmlsConnector,
  val dataCacheConnector: DataCacheConnector,
  val enrolmentsService: AuthEnrolmentsService,
  val sectionsProvider: SectionsProvider,
  val environment: Environment,
  clock: Clock
) extends Logging {
  private val renewalPeriod = 30

  val Pending      = "Pending"
  val Approved     = "Approved"
  val Rejected     = "Rejected"
  val Revoked      = "Revoked"
  val Expired      = "Expired"
  val Withdrawal   = "Withdrawal"
  val DeRegistered = "De-Registered"

  private def getApprovedStatus(response: ReadStatusResponse) =
    (response.currentRegYearEndDate, response.renewalConFlag) match {
      case (Some(endDate), false) if LocalDate.now(clock.withZone(UTC)).isAfter(endDate.minusDays(renewalPeriod)) =>
        ReadyForRenewal(response.currentRegYearEndDate)
      case (_, true)                                                                                              => RenewalSubmitted(response.currentRegYearEndDate)
      case _                                                                                                      => SubmissionDecisionApproved
    }

  private def getETMPStatus(response: ReadStatusResponse) = {
    // $COVERAGE-OFF$
    logger.debug("StatusService:getETMPStatus:formBundleStatus:" + response.formBundleStatus)
    // $COVERAGE-ON$
    response.formBundleStatus match {
      case `Pending`                         => SubmissionReadyForReview
      case `Approved`                        => getApprovedStatus(response)
      case `Rejected`                        => SubmissionDecisionRejected
      case `Revoked`                         => SubmissionDecisionRevoked
      case `Expired`                         => SubmissionDecisionExpired
      case `Withdrawal`                      => SubmissionWithdrawn
      case `DeRegistered`                    => models.status.DeRegistered
      case _ if environment.mode == Mode.Dev => models.status.NotCompleted
      case status: String                    => throw new RuntimeException(s"ETMP returned status is inconsistent [status:$status]")
    }
  }

  def getSafeIdFromReadStatus(mlrRegNumber: String, accountTypeId: (String, String), credId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[String]] =
    dataCacheConnector
      .fetch[BusinessMatching](credId, BusinessMatching.key)
      .map { optBusinessMatching =>
        optBusinessMatching.flatMap(businessMatching =>
          businessMatching.reviewDetails.map(reviewDetails => reviewDetails.safeId)
        )
      }
      .flatMap {
        case Some(safeId) => Future.successful(Option(safeId))
        case None         =>
          amlsConnector.status(mlrRegNumber, accountTypeId) map { response =>
            // $COVERAGE-OFF$
            logger.debug("StatusService:etmpStatusInformation:response:" + response)
            // $COVERAGE-ON$
            Option(response.safeId.getOrElse(""))
          }
      }

  private def etmpStatusInformation(mlrRegNumber: String, accountTypeId: (String, String))(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[(SubmissionStatus, Option[ReadStatusResponse])] =
    amlsConnector.status(mlrRegNumber, accountTypeId) map { response =>
      val status = getETMPStatus(response)
      // $COVERAGE-OFF$
      logger.debug("StatusService:etmpStatusInformation:status:" + status)
      // $COVERAGE-ON$
      (status, Some(response))
    }

  def getDetailedStatus(amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    messages: Messages
  ): Future[(SubmissionStatus, Option[ReadStatusResponse])] =
    amlsRegistrationNumber match {
      case Some(mlrRegNumber) =>
        // $COVERAGE-OFF$
        logger.debug("StatusService:getDetailedStatus:mlrRegNumber:" + mlrRegNumber)
        // $COVERAGE-ON$
        etmpStatusInformation(mlrRegNumber, accountTypeId)(hc, ec)
      case None               =>
        // $COVERAGE-OFF$
        logger.debug("StatusService:getDetailedStatus: No mlrRegNumber")
        // $COVERAGE-ON$
        notYetSubmitted(cacheId)(ec, messages) map { status =>
          (status, None)
        }
    }

  def getStatus(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    messages: Messages
  ): Future[SubmissionStatus] =
    amlsRegistrationNo match {
      case Some(mlrRegNumber) =>
        // $COVERAGE-OFF$
        logger.debug("StatusService:getStatus:mlrRegNumber:" + mlrRegNumber)
        // $COVERAGE-ON$
        etmpStatus(mlrRegNumber, accountTypeId)(hc, ec)
      case None               =>
        // $COVERAGE-OFF$
        logger.debug("StatusService:getStatus: No mlrRegNumber")
        // $COVERAGE-ON$
        notYetSubmitted(credId)(ec, messages)
    }

  def getStatus(mlrRegNumber: String, accountTypeId: (String, String))(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[SubmissionStatus] =
    etmpStatus(mlrRegNumber, accountTypeId)(hc, ec)

  def getReadStatus(amlsRegistrationNumber: Option[String], accountTypeId: (String, String))(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[ReadStatusResponse] =
    amlsRegistrationNumber match {
      case Some(mlrRegNumber) =>
        // $COVERAGE-OFF$
        logger.debug("StatusService:getReadStatus:mlrRegNumber:" + mlrRegNumber)
        // $COVERAGE-ON$
        etmpReadStatus(mlrRegNumber, accountTypeId)(hc, ec)
      case _                  => throw new RuntimeException("ETMP returned no read status")
    }

  def getReadStatus(amlsRegistrationNumber: String, accountTypeId: (String, String))(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[ReadStatusResponse] = etmpReadStatus(amlsRegistrationNumber, accountTypeId)

  private def notYetSubmitted(
    cacheId: String
  )(implicit ec: ExecutionContext, messages: Messages): Future[SubmissionStatus] = {

    def isComplete(seq: Seq[TaskRow]): Boolean =
      seq forall { row =>
        row.status == Completed || row.status == Updated
      }

    sectionsProvider.taskRows(cacheId) map { sections =>
      if (isComplete(sections)) {
        // $COVERAGE-OFF$
        logger.debug("StatusService:notYetSubmitted: SubmissionReady")
        // $COVERAGE-ON$
        SubmissionReady
      } else {
        // $COVERAGE-OFF$
        logger.debug("StatusService:notYetSubmitted: NotCompleted")
        // $COVERAGE-ON$
        NotCompleted
      }
    }
  }

  private def etmpStatus(amlsRefNumber: String, accountTypeId: (String, String))(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[SubmissionStatus] =
    amlsConnector.status(amlsRefNumber, accountTypeId) map { response =>
      getETMPStatus(response)
    }

  private def etmpReadStatus(amlsRefNumber: String, accountTypeId: (String, String))
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ReadStatusResponse] = {
    {
      val status = amlsConnector.status(amlsRefNumber, accountTypeId)
      // $COVERAGE-OFF$
      logger.debug("StatusService:etmpReadStatus:status:" + status)
      // $COVERAGE-ON$
      status
    }
  }

  def isPending(status: SubmissionStatus): Boolean = status match {
    case SubmissionReadyForReview | RenewalSubmitted(_) => true
    case _                                              => false
  }

  def isPreSubmission(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    messages: Messages
  ): Future[Boolean] =
    getStatus(amlsRegistrationNo, accountTypeId, credId) map isPreSubmission

  def isPreSubmission(status: SubmissionStatus): Boolean = Set(NotCompleted, SubmissionReady).contains(status)
}
