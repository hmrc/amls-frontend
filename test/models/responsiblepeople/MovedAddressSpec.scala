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

package models.responsiblepeople

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class MovedAddressSpec extends PlaySpec with MockitoSugar {
  
  "MovedAddress" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "movedAddress" -> Seq("true")
        )

        MovedAddress.formRule.validate(data) must
          be(Valid(MovedAddress(true)))
      }

      "given a 'false' value" in {

        val data = Map(
          "movedAddress" -> Seq("false")
        )

        MovedAddress.formRule.validate(data) must
          be(Valid(MovedAddress(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        MovedAddress.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "movedAddress") -> Seq(ValidationError("error.required.rp.moved.address"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "movedAddress" -> Seq("")
        )

        MovedAddress.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "movedAddress") -> Seq(ValidationError("error.required.rp.moved.address"))
          )))
      }
    }

    "write correct data" in {

      val model = MovedAddress(true)

      MovedAddress.formWrites.writes(model) must
        be(Map(
          "movedAddress" -> Seq("true")
        ))
    }
  }
}
