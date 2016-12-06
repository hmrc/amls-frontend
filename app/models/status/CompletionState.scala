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

  def notificationsAvailable : Boolean = {
    Seq(SubmissionReadyForReview, SubmissionDecisionApproved, SubmissionDecisionRejected).contains(currentState)
  }
}

object CompletionStateViewModel {

  def apply(current: SubmissionStatus): CompletionStateViewModel = {
    val statuses = ListMap(NotCompleted -> Complete,
      SubmissionReady -> Complete,
      SubmissionReadyForReview -> Complete,
      SubmissionDecisionApproved -> Complete,
      SubmissionDecisionRejected -> Complete)

    val (prefix, suffix) = statuses.span(s => s._1 != current)
    val updatedStatuses = prefix ++
                          Map(current -> Current) ++
                          suffix.tail.mapValues(_ => Incomplete)


    val filterStatuses = current match {
      case SubmissionDecisionRejected => updatedStatuses.filterNot(m => m._1 == SubmissionDecisionApproved)
      case _ => updatedStatuses.filterNot (m => m._1 == SubmissionDecisionRejected)
    }

    CompletionStateViewModel(filterStatuses)
  }


}


