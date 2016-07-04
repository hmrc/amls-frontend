package models.hvd

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import play.api.libs.json.Json


class HowWillYouSellGoodsSpec extends WordSpec with MustMatchers{

  val fullData = HowWillYouSellGoods(Seq(Wholesale, Retail, Auction))
  val fullForm = Map (
      "salesChannels[0]" -> Seq("Wholesale"),
      "salesChannels[1]" -> Seq("Retail"),
      "salesChannels[2]" -> Seq("Auction")
  )

  "How will You Sell Goods" should {
    "Round trip through Json" in {
      val j = Json.toJson(fullData)
      j.as[HowWillYouSellGoods] must be (fullData)
    }

    "write to the expected form"  in {
      HowWillYouSellGoods.writeF.writes(fullData)  must be (fullForm)
    }
  }
}
