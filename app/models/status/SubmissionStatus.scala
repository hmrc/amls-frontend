package models.status

import org.joda.time.LocalDate

sealed trait SubmissionStatus

object NotCompleted extends SubmissionStatus
object SubmissionReady extends SubmissionStatus
object SubmissionReadyForReview extends SubmissionStatus
object SubmissionDecisionApproved extends SubmissionStatus
object SubmissionDecisionRejected extends SubmissionStatus
object SubmissionDecisionRevoked extends SubmissionStatus
object SubmissionDecisionExpired extends SubmissionStatus
case class ReadyForRenewal(renewalDate: Option[LocalDate]) extends SubmissionStatus
case class RenewalSubmitted(renewalDate: Option[LocalDate])  extends SubmissionStatus

