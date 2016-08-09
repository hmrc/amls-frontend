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
      val model = CompletionStateViewModel(NotSubmitted)
      model.statuses.get(NotSubmitted).get must be(Current)

      model.statuses.filter(entry => entry._1 != NotSubmitted) map {
        entry => entry._2 must be(Incomplete)
      }
    }

    "return updated statuses when submission fee paid" in {
      val model = CompletionStateViewModel(SubmissionFeePaid)
      model.statuses.get(SubmissionFeePaid).get must be(Current)

      model.statuses.span(s => s._1 != SubmissionFeePaid)._1 map {
        entry => entry._2 must be(Complete)
      }
      model.statuses.span(s => s._1 != SubmissionFeePaid)._2.drop(1) map {
        entry => entry._2 must be(Incomplete)
      }
    }
  }

}
