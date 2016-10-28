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
    val combo = (status, statuses(status))

    combo match {
      case (NotCompleted, Incomplete) | (NotCompleted, Current) => Messages("status.incomplete")
      case (NotCompleted, Complete) => Messages("status.complete")
      case (SubmissionReady, Incomplete) | (SubmissionReady, Current) => Messages("status.notsubmitted")
      case (SubmissionReady, Complete) => Messages("status.submitted")
      case (SubmissionReadyForReview, _) => Messages("status.underreview")
      case (SubmissionDecisionApproved, _) => Messages("status.decisionmade")
      case (SubmissionDecisionRejected, _) => Messages("status.decisionmade")
      case _ => "Not found"
    }
  }

  def currentState = {
    (statuses.find(_._2 == Current) map {
      pair: (SubmissionStatus,CompletionState) => {
        pair._1
      }
    }).getOrElse(NotCompleted)
  }

}

object CompletionStateViewModel {

  def apply(current: SubmissionStatus): CompletionStateViewModel = {
    val statuses = ListMap(NotCompleted -> Complete,
      SubmissionReady -> Complete,
      SubmissionReadyForReview -> Complete,
      SubmissionDecisionApproved -> Complete,
      SubmissionDecisionRejected -> Complete)

    val updatedStatuses = statuses.span(s => s._1 != current)._1 ++ Map(current -> Current) ++ statuses.span(s => s._1 != current)._2.drop(1).map {
      status => (status._1, Incomplete)
    }

    val filterStatuses = current match{
      case SubmissionDecisionRejected => updatedStatuses.filterNot(m => m._1 == SubmissionDecisionApproved)
      case _ => updatedStatuses.filterNot(m => m._1 == SubmissionDecisionRejected)
    }

    CompletionStateViewModel(filterStatuses)
  }


}


