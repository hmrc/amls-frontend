package models.status

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class CompletionStateSpec  extends PlaySpec with MockitoSugar{

  "Completion State View Model" should {
    "return updated statuses when SubmissionDecisionMade" in {
      val model = CompletionStateViewModel(SubmissionDecisionMade)
      model.statuses.get(SubmissionDecisionMade).get must be(Current)

      model.statuses.filter(entry => entry._1 != SubmissionDecisionMade) map {
        entry => entry._2 must be(Complete)
      }
    }

    "return updated statuses when Not Submitted" in {
      val model = CompletionStateViewModel(NotCompleted)
      model.statuses.get(NotCompleted).get must be(Current)

      model.statuses.filter(entry => entry._1 != NotCompleted) map {
        entry => entry._2 must be(Incomplete)
      }
    }

    "return updated statuses when submission fee paid" in {
      val model = CompletionStateViewModel(SubmissionFeesDue)
      model.statuses.get(SubmissionFeesDue).get must be(Current)

      model.statuses.span(s => s._1 != SubmissionFeesDue)._1 map {
        entry => entry._2 must be(Complete)
      }
      model.statuses.span(s => s._1 != SubmissionFeesDue)._2.drop(1) map {
        entry => entry._2 must be(Incomplete)
      }
    }
  }

}
