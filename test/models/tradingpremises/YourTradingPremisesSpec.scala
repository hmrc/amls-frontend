/*
 * Copyright 2024 HM Revenue & Customs
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

import models.DateOfChange
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

import java.time.LocalDate

class YourTradingPremisesSpec extends AnyWordSpec with Matchers {

  val model = YourTradingPremises(
    "foo",
    Address(
      "1",
      Some("2"),
      None,
      None,
      "AA11 1AA"
    ),
    Some(true),
    Some(LocalDate.of(1990, 2, 24))
  )

  "YourTradingPremises" must {

    val json = Json.obj(
      "tradingName"           -> "foo",
      "addressLine1"          -> "1",
      "addressLine2"          -> "2",
      "addressDateOfChange"   -> LocalDate.of(1997, 7, 1),
      "postcode"              -> "AA11 1AA",
      "isResidential"         -> true,
      "startDate"             -> LocalDate.of(1990, 2, 24),
      "tradingNameChangeDate" -> LocalDate.of(2016, 1, 12)
    )

    val jsonModel = model.copy(
      tradingNameChangeDate = Some(DateOfChange(LocalDate.of(2016, 1, 12))),
      tradingPremisesAddress =
        model.tradingPremisesAddress.copy(dateOfChange = Some(DateOfChange(LocalDate.of(1997, 7, 1))))
    )

    "Correctly serialise from json" in {
      implicitly[Reads[YourTradingPremises]].reads(json) must
        be(JsSuccess(jsonModel))
    }

    "Correctly write form model to json" in {

      implicitly[Writes[YourTradingPremises]].writes(jsonModel) must
        be(json)
    }
  }
}
