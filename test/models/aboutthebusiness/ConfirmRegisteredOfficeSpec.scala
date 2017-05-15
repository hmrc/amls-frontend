/*
 * Copyright 2017 HM Revenue & Customs
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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.Path
import jto.validation.ValidationError

class ConfirmRegisteredOfficeSpec extends PlaySpec with MockitoSugar {
  "RegOfficeOrMainPlaceOfBusiness" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
        )

        ConfirmRegisteredOffice.formRule.validate(data) must
          be(Valid(ConfirmRegisteredOffice(true)))
      }

      "given a 'false' value" in {

        val data = Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("false")
        )

        ConfirmRegisteredOffice.formRule.validate(data) must
          be(Valid(ConfirmRegisteredOffice(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        ConfirmRegisteredOffice.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("error.required.atb.confirm.office"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("")
        )

        ConfirmRegisteredOffice.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("error.required.atb.confirm.office"))
          )))
      }
    }

    "write correct data" in {

      val model = ConfirmRegisteredOffice(true)

      ConfirmRegisteredOffice.formWrites.writes(model) must
        be(Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
        ))
    }
  }
}
