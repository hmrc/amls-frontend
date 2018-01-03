/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.businessmatching

import org.scalatestplus.play.PlaySpec
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

class MsbServicesSpec extends PlaySpec {

  "MsbServices" must {
    import jto.validation.forms.Rules._
    "round trip through Json correctly" in {

      val data = MsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange))
      val js = Json.toJson(data)
      js.as[MsbServices] mustEqual data
    }

    "round trip through Forms correctly" in {

      val model = MsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange))
      val data = implicitly[Write[MsbServices, UrlFormEncoded]].writes(model)

      implicitly[Rule[UrlFormEncoded, MsbServices]].validate(data) mustEqual Valid(model)
    }

    "fail validation" when {
      "the Map is empty" in {

        implicitly[Rule[UrlFormEncoded, MsbServices]].validate(Map.empty)
          .mustEqual(Invalid(Seq((Path \ "msbServices") -> Seq(ValidationError("error.required.msb.services")))))
      }

      "the set is empty" in {

        val data: UrlFormEncoded = Map(
          "msbServices" -> Seq.empty[String]
        )

        implicitly[Rule[UrlFormEncoded, MsbServices]].validate(data)
          .mustEqual(Invalid(Seq((Path \ "msbServices") -> Seq(ValidationError("error.required.msb.services")))))
      }

      "there is an invalid entry in the set" in {

        val data: UrlFormEncoded = Map(
          "msbServices" -> Seq("invalid")
        )

        implicitly[Rule[UrlFormEncoded, MsbServices]].validate(data)
          .mustEqual(Invalid(Seq((Path \ "msbServices" \ 0) -> Seq(ValidationError("error.invalid")))))
      }
    }

    "serialize with the expected structure" in {

      val model = MsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange))

      MsbServices.formWrites.writes(model) mustEqual Map(
        "msbServices[]" -> Seq("01", "03", "04", "02")
      )
    }
  }
}
