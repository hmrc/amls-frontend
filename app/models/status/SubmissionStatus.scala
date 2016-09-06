package models.status

sealed trait SubmissionStatus {

}

object NotCompleted extends SubmissionStatus
object SubmissionReady extends SubmissionStatus
object SubmissionReadyForReview extends SubmissionStatus
object SubmissionDecisionApproved extends SubmissionStatus
object SubmissionDecisionRejected extends SubmissionStatus
