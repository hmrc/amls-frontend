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

package models.aboutthebusiness

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class LettersAddressSpec extends PlaySpec with MockitoSugar {
  "LettersAddressSpec" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "lettersAddress" -> Seq("true")
        )

        LettersAddress.formRule.validate(data) must
          be(Valid(LettersAddress(true)))
      }

      "given a 'false' value" in {

        val data = Map(
          "lettersAddress" -> Seq("false")
        )

        LettersAddress.formRule.validate(data) must
          be(Valid(LettersAddress(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        LettersAddress.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "lettersAddress") -> Seq(ValidationError("error.required.atb.lettersaddress"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "lettersAddress" -> Seq("")
        )

        LettersAddress.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "lettersAddress") -> Seq(ValidationError("error.required.atb.lettersaddress"))
          )))
      }
    }

    "write correct data" in {

      val model = LettersAddress(true)

      LettersAddress.formWrites.writes(model) must
        be(Map(
          "lettersAddress" -> Seq("true")
        ))
    }
  }
}
