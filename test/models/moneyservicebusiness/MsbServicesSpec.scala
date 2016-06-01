package models.moneyservicebusiness

import org.scalatest.WordSpec
import org.specs2.matcher.MustMatchers
import play.api.libs.json.Json

class MsbServicesSpec extends WordSpec with MustMatchers{
  "MsbServices" should {
    "round trip through Json correctly" in {
      val data = MsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange ))
      val js = Json.toJson(data)
      js.as[MsbServices] must be (data)
    }
  }
}
