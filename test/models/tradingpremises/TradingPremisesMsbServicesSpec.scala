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

package models.tradingpremises

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import utils.AmlsSpec

class TradingPremisesMsbServicesSpec extends AmlsSpec {

  "MsbServices" must {

    "round trip through Json correctly" in {
      val data = TradingPremisesMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, ForeignExchange))
      val js = Json.toJson(data)

      js.as[TradingPremisesMsbServices] mustEqual data
    }

    "round trip through Forms correctly" in {

      val model = TradingPremisesMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, ForeignExchange))
      val data = implicitly[Write[TradingPremisesMsbServices, UrlFormEncoded]].writes(model)

      implicitly[Rule[UrlFormEncoded, TradingPremisesMsbServices]].validate(data) mustEqual Valid(model)
    }

    "fail to validate when the set is empty" in {

      val data: UrlFormEncoded = Map(
        "msbServices" -> Seq.empty[String]
      )

      implicitly[Rule[UrlFormEncoded, TradingPremisesMsbServices]].validate(data)
          .mustEqual(Invalid(Seq((Path \ "msbServices") -> Seq(ValidationError("error.required.tp.services")))))
    }

    "fail to validate when there is an invalid entry in the set" in {

      val data: UrlFormEncoded = Map(
        "msbServices" -> Seq("invalid")
      )

      implicitly[Rule[UrlFormEncoded, TradingPremisesMsbServices]].validate(data)
          .mustEqual(Invalid(Seq((Path \ "msbServices" \ 0) -> Seq(ValidationError("error.invalid")))))
    }

    "serialize with the expected structure" in {

      val model = TradingPremisesMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, ForeignExchange))

      val serializedModel = TradingPremisesMsbServices.formW.writes(model)

      serializedModel.getOrElse("msbServices[]", Seq()).toSet mustEqual Set("01", "02", "03", "04", "05")
    }
  }
}
