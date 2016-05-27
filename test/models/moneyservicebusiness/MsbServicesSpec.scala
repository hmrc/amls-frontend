package models.moneyservicebusiness

import org.scalatest.WordSpec
import org.scalatestplus.play.PlaySpec
import org.specs2.matcher.MustMatchers
import play.api.libs.json.Json
import utils.JsonMapping

class MsbServicesSpec extends PlaySpec {

  "MsbServices" must {

    "round trip through Json correctly" in new JsonMapping {

      import play.api.data.mapping.json.Writes._

      val data = MsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange))
      val js = Json.toJson(data)

      js.as[MsbServices] must be (data)
    }
  }
}
