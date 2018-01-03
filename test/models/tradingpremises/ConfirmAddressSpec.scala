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

package models.tradingpremises

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class ConfirmAddressSpec extends PlaySpec with MockitoSugar {
  
  "ConfirmAddress" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "confirmAddress" -> Seq("true")
        )

        ConfirmAddress.formRule.validate(data) must
          be(Valid(ConfirmAddress(true)))
      }

      "given a 'false' value" in {

        val data = Map(
          "confirmAddress" -> Seq("false")
        )

        ConfirmAddress.formRule.validate(data) must
          be(Valid(ConfirmAddress(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        ConfirmAddress.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "confirmAddress") -> Seq(ValidationError("error.required.tp.confirm.address"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "confirmAddress" -> Seq("")
        )

        ConfirmAddress.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "confirmAddress") -> Seq(ValidationError("error.required.tp.confirm.address"))
          )))
      }
    }

    "write correct data" in {

      val model = ConfirmAddress(true)

      ConfirmAddress.formWrites.writes(model) must
        be(Map(
          "confirmAddress" -> Seq("true")
        ))
    }
  }
}
