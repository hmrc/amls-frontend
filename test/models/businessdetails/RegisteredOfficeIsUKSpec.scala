/*
 * Copyright 2023 HM Revenue & Customs
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

package models.businessdetails

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class RegisteredOfficeIsUKSpec extends PlaySpec with MockitoSugar {
  "RegisteredOfficeIsUK" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "isUK" -> Seq("true")
        )

        RegisteredOfficeIsUK.formRule.validate(data) must
          be(Valid(RegisteredOfficeIsUK(true)))
      }

      "given a 'false' value" in {

        val data = Map(
          "isUK" -> Seq("false")
        )

        RegisteredOfficeIsUK.formRule.validate(data) must
          be(Valid(RegisteredOfficeIsUK(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        RegisteredOfficeIsUK.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.atb.registered.office.uk.or.overseas"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "isUK" -> Seq("")
        )

        RegisteredOfficeIsUK.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.atb.registered.office.uk.or.overseas"))
          )))
      }
    }

    "write correct data" in {

      val model = RegisteredOfficeIsUK(true)

      RegisteredOfficeIsUK.formWrites.writes(model) must
        be(Map(
          "isUK" -> Seq("true")
        ))
    }
  }
}
