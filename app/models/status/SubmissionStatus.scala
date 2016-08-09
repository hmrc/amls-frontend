package models.status

sealed trait SubmissionStatus {

}

object NotSubmitted extends SubmissionStatus
object SubmissionSubmitted extends SubmissionStatus
object SubmissionFeePaid extends SubmissionStatus
object SubmissionUnderReview extends SubmissionStatus
object SubmissionDecisionMade extends SubmissionStatus
