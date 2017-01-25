package models.hvd

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import jto.validation.{Path, Failure, Success}
import jto.validation.ValidationError
import play.api.libs.json.Json


class HowWillYouSellGoodsSpec extends WordSpec with MustMatchers{

  val fullData = HowWillYouSellGoods(Seq(Wholesale, Retail, Auction))
  val fullForm = Map (
      "salesChannels[]" -> Seq("Wholesale","Retail","Auction")
  )

  "How will You Sell Goods" should {
    "Round trip through Json" in {
      val j = Json.toJson(fullData)
      j.as[HowWillYouSellGoods] must be (fullData)
    }

    "write to the expected form"  in {
      HowWillYouSellGoods.formW.writes(fullData)  must be (fullForm)
    }

    "read from the expected form"  in {
      HowWillYouSellGoods.formR.validate(fullForm)  must be (Success(fullData))
    }

    "round trip through Url encoded form" in {
      HowWillYouSellGoods.formW.writes(
        HowWillYouSellGoods.formR.validate(fullForm) match {
          case Success(x) => x
          case _ => HowWillYouSellGoods(Seq.empty)
        }
      ) must be (fullForm)

      HowWillYouSellGoods.formR.validate(
        HowWillYouSellGoods.formW.writes(fullData)
      ) must be (Success(fullData))
    } 

    "fail form validation of no channels are selected" in {
      val testForm = Map[String,Seq[String]]()

      HowWillYouSellGoods.formR.validate(testForm) must be (Failure(Seq((Path \ "salesChannels") -> Seq(ValidationError("error.required.hvd.how-will-you-sell-goods")))))
    }
  }
}
