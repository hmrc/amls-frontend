/*
 * Copyright 2021 HM Revenue & Customs
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

class BusinessMatchingMsbServicesSpec extends PlaySpec {

  "MsbServices" must {
    import jto.validation.forms.Rules._
    "round trip through Json correctly" in {

      val data = BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, ForeignExchange))
      val js = Json.toJson(data)
      js.as[BusinessMatchingMsbServices] mustEqual data
    }

    "round trip through Forms correctly" in {

      val model = BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, ForeignExchange))
      val data = implicitly[Write[BusinessMatchingMsbServices, UrlFormEncoded]].writes(model)

      implicitly[Rule[UrlFormEncoded, BusinessMatchingMsbServices]].validate(data) mustEqual Valid(model)
    }

    "fail validation" when {
      "the Map is empty" in {

        implicitly[Rule[UrlFormEncoded, BusinessMatchingMsbServices]].validate(Map.empty)
          .mustEqual(Invalid(Seq((Path \ "msbServices") -> Seq(ValidationError("error.required.msb.services")))))
      }

      "the set is empty" in {

        val data: UrlFormEncoded = Map(
          "msbServices" -> Seq.empty[String]
        )

        implicitly[Rule[UrlFormEncoded, BusinessMatchingMsbServices]].validate(data)
          .mustEqual(Invalid(Seq((Path \ "msbServices") -> Seq(ValidationError("error.required.msb.services")))))
      }

      "the Map is empty on tp question" in {

        BusinessMatchingMsbServices.formReadsTP.validate(Map.empty)
          .mustEqual(Invalid(Seq((Path \ "msbServices") -> Seq(ValidationError("error.required.msb.services.tp")))))
      }

      "the set is empty on tp question" in {

        val data: UrlFormEncoded = Map(
          "msbServices" -> Seq.empty[String]
        )

        BusinessMatchingMsbServices.formReadsTP.validate(data)
          .mustEqual(Invalid(Seq((Path \ "msbServices") -> Seq(ValidationError("error.required.msb.services.tp")))))
      }

      "there is an invalid entry in the set" in {

        val data: UrlFormEncoded = Map(
          "msbServices" -> Seq("invalid")
        )

        implicitly[Rule[UrlFormEncoded, BusinessMatchingMsbServices]].validate(data)
          .mustEqual(Invalid(Seq((Path \ "msbServices" \ 0) -> Seq(ValidationError("error.invalid")))))
      }
    }

    "serialize with the expected structure" in {

      val model = BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, ForeignExchange))

      val serializedModel = BusinessMatchingMsbServices.formWrites.writes(model)

      serializedModel.getOrElse("msbServices[]", Seq()).toSet mustEqual Set("01", "02", "03", "04", "05")
    }
  }
}
