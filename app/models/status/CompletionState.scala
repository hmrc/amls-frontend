package models.status

import play.api.i18n.Messages

import scala.collection.immutable.ListMap

sealed trait CompletionState {
  def cssClass: String
}

object Current extends CompletionState {
  val cssClass = "current"
}

object Complete extends CompletionState {
  val cssClass = "status-list--complete"
}

object Incomplete extends CompletionState {
  val cssClass = "status-list--upcoming"
}


case class CompletionStateViewModel(statuses: Map[SubmissionStatus, CompletionState]) {

  def statusTitle(status: SubmissionStatus) = {
    val combo = (status, statuses.get(status).get)

    combo match {
      case (NotSubmitted, Incomplete) | (NotSubmitted, Current) => Messages("status.incomplete")
      case (NotSubmitted, Complete) => Messages("status.complete")
      case (SubmissionSubmitted, Incomplete) | (SubmissionSubmitted, Current) => Messages("status.notsubmitted")
      case (SubmissionSubmitted, Complete) => Messages("status.submitted")
      case (SubmissionFeePaid, _) => Messages("status.feepaid")
      case (SubmissionUnderReview, _) => Messages("status.underreview")
      case (SubmissionDecisionMade, _) => Messages("status.decisionmade")
      case _ => "Not found"
    }
  }

}

object CompletionStateViewModel {

  def apply(current: SubmissionStatus): CompletionStateViewModel = {
    val statuses = ListMap(NotSubmitted -> Complete,
      SubmissionSubmitted -> Complete,
      SubmissionFeePaid -> Complete,
      SubmissionUnderReview -> Complete,
      SubmissionDecisionMade -> Complete)

    val updatedStatuses = statuses.span(s => s._1 != current)._1 ++ Map(current -> Current) ++ statuses.span(s => s._1 != current)._2.drop(1).map {
      status => (status._1, Incomplete)
    }

    CompletionStateViewModel(updatedStatuses)
  }


}


