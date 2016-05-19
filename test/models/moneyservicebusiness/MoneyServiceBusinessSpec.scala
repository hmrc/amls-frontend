package models.moneyservicebusiness

import org.scalatest.Pending
import org.scalatestplus.play.PlaySpec
import typeclasses.MongoKey

class MoneyServiceBusinessSpec extends PlaySpec {
  "MoneyServiceBusiness" should {
    "have an implicit conversion from Option which" when {
      "called with None" should {
        "return a default version of MoneyServiceBusiness" in {
          val res:MoneyServiceBusiness = None
          res must be (MoneyServiceBusiness())
        }
      }

      "called with a concrete value" should {
        "return the value passed in extracted from the option" in {
          val res:MoneyServiceBusiness = Some(MoneyServiceBusiness(Some(MsbServices(Seq(ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)))))
          res must be (MoneyServiceBusiness(Some(MsbServices(Seq(ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)))))
        }
      }
    }

    "Provide an implicit mongo-key" in {
      def x(implicit mongoKey: MongoKey[MoneyServiceBusiness]) = {
        mongoKey()
      }

      val res = x
      res must be("money-service-business")
    }

    "have a section function that" must {
      "return a NotStarted Section when model is empty" in Pending
      "return a Completed Section when model is complete" in Pending
      "return a Started Section when model is incomplete" in Pending
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in Pending

      "correctly show if the model is incomplete" in Pending
    }
  }
}
