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

class ContactingYouEmailSpec extends PlaySpec with MockitoSugar {
  "ContactingYouEmailSpec" must {

    "successfully validate" when {
      "given a 'matchinig emails" in {

        val data = Map(
          "email" -> Seq("test@test.com"),
          "confirmEmail" -> Seq("test@test.com")

        )

        ContactingYouEmail.formRule.validate(data) must
          be(Valid(ContactingYouEmail("test@test.com","test@test.com")))
      }

    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        ContactingYouEmail.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "email") -> Seq(ValidationError("error.required")),
            (Path \ "confirmEmail") -> Seq(ValidationError("error.required"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "email" -> Seq(""),
          "confirmEmail" -> Seq("")
        )

        ContactingYouEmail.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "email") -> Seq(ValidationError("error.required.rp.email")),
            (Path \ "confirmEmail") -> Seq(ValidationError("error.required.rp.email"))
          )))
      }
    }

    "write correct data" in {

      val model = ContactingYouEmail("test@test.com","test@test.com")

      ContactingYouEmail.formWrites.writes(model) must
        be(Map(
          "email" -> Seq("test@test.com"),
          "confirmEmail" -> Seq("test@test.com")
        ))
    }
  }
}
