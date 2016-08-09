package models.status

sealed trait SubmissionStatus {

}

object NotCompleted extends SubmissionStatus
object SubmissionReady extends SubmissionStatus
object SubmissionFeesDue extends SubmissionStatus
object SubmissionReadyForReview extends SubmissionStatus
object SubmissionDecisionMade extends SubmissionStatus
