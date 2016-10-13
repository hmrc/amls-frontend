package models.businessmatching

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

class MsbServicesSpec extends PlaySpec {

  "MsbServices" must {

    "round trip through Json correctly" in {

      val data = MsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange))
      val js = Json.toJson(data)

      js.as[MsbServices] mustEqual data
    }

    "round trip through Forms correctly" in {

      val model = MsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange))
      val data = implicitly[Write[MsbServices, UrlFormEncoded]].writes(model)

      implicitly[Rule[UrlFormEncoded, MsbServices]].validate(data) mustEqual Success(model)
    }

    "fail to validate when the set is empty" in {

      val data: UrlFormEncoded = Map(
        "msbServices" -> Seq.empty[String]
      )

      implicitly[Rule[UrlFormEncoded, MsbServices]].validate(data)
          .mustEqual(Failure(Seq((Path \ "msbServices") -> Seq(ValidationError("error.required.msb.services")))))
    }

    "fail to validate when there is an invalid entry in the set" in {

      val data: UrlFormEncoded = Map(
        "msbServices" -> Seq("invalid")
      )

      implicitly[Rule[UrlFormEncoded, MsbServices]].validate(data)
          .mustEqual(Failure(Seq((Path \ "msbServices" \ 0) -> Seq(ValidationError("error.invalid")))))
    }

    "serialize with the expected structure" in {

      val model = MsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange))

      MsbServices.formW.writes(model) mustEqual Map(
        "msbServices[]" -> Seq("01", "03", "04", "02")
      )
    }
  }
}
