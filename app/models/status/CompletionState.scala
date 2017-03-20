package models.status

import models.status
import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

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

object NotShown extends CompletionState {
  val cssClass = "status-list--hidden"
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
    (statuses.find(x => x._2 == Current || x._2 == NotShown) map {
      pair: (SubmissionStatus, CompletionState) => {
        pair._1
      }
    }).getOrElse(NotCompleted)
  }

  def notificationsAvailable: Boolean = {
    currentState match {
      case SubmissionReadyForReview|SubmissionDecisionApproved|SubmissionDecisionRejected|ReadyForRenewal(_)|RenewalSubmitted(_) => true
      case _ => false
    }
  }
}

object CompletionStateViewModel {

  def apply(current: SubmissionStatus): CompletionStateViewModel = {
    val statuses = ListMap(NotCompleted -> Complete,
      SubmissionReady -> Complete,
      SubmissionReadyForReview -> Complete,
      SubmissionDecisionApproved -> Complete,
      SubmissionDecisionRejected -> Complete) ++ {
      current match {
        case r: ReadyForRenewal => ListMap(r -> NotShown)
        case r: RenewalSubmitted => ListMap(r -> NotShown)
        case _ => ListMap.empty
      }
    }

    val (prefix, suffix) = statuses.span(s => s._1 != current)
    val updatedStatuses = {
      prefix ++
        Map(current -> Current) ++
        suffix.tail.mapValues(_ => Incomplete)


    }


    val filterStatuses = current match {
      case SubmissionDecisionRejected => updatedStatuses.filterNot(m => m._1 == SubmissionDecisionApproved)
      case _ => updatedStatuses.filterNot(m => m._1 == SubmissionDecisionRejected)
    }
    CompletionStateViewModel(filterStatuses)
  }


}


