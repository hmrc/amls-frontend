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
import jto.validation.{Path, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class ContactingYouSpec extends PlaySpec with MockitoSugar {
  "Contacting You Form Details" must {

    "successfully validate" when {
      "given a valid phone number, email and letterToThisAddress 'true' value" in {

        val data = Map(
          "phoneNumber" -> Seq("1234567890"),
          "email" -> Seq("test@test.com"),
          "letterToThisAddress" -> Seq("true")
        )

        ContactingYouForm.formRule.validate(data) must
          be(Valid(ContactingYouForm("1234567890", "test@test.com", true)))
      }

      "given a valid phone number, email and letterToThisAddress 'false' value" in {

        val data = Map(
          "phoneNumber" -> Seq("1234567890"),
          "email" -> Seq("test@test.com"),
          "letterToThisAddress" -> Seq("false")
        )

        ContactingYouForm.formRule.validate(data) must
          be(Valid(ContactingYouForm("1234567890", "test@test.com", false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty map" in {

        ContactingYouForm.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "phoneNumber") -> Seq(ValidationError("error.required")),
            (Path \ "email") -> Seq(ValidationError("error.required")),
            (Path \ "letterToThisAddress") -> Seq(ValidationError("error.required.rightaddress"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "phoneNumber" -> Seq(""),
          "email" -> Seq(""),
          "letterToThisAddress" -> Seq("")
        )

        ContactingYouForm.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "phoneNumber") -> Seq(ValidationError("error.required.phone.number")),
            (Path \ "email") -> Seq(ValidationError("error.required.rp.email")),
            (Path \ "letterToThisAddress") -> Seq(ValidationError("error.required.rightaddress"))
          )))
      }

      "given invalid phone number and email containing too many characters" in {

        val data = Map(
          "phoneNumber" -> Seq("1" * 31),
          "email" -> Seq("a" * 101 + "@test.com"),
          "letterToThisAddress" -> Seq("true")
        )

        ContactingYouForm.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "phoneNumber") -> Seq(ValidationError("error.max.length.phone")),
            (Path \ "email") -> Seq(ValidationError("error.max.length.rp.email"))
          )))
      }

      "given invalid phone number containing non-numeric characters" in {

        val data = Map(
          "phoneNumber" -> Seq("12ab34cd"),
          "email" -> Seq("test@test.com"),
          "letterToThisAddress" -> Seq("true")
        )

        ContactingYouForm.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "phoneNumber") -> Seq(ValidationError("err.invalid.phone.number"))
          )))
      }
    }

    val completeJson = Json.obj(
      "phoneNumber" -> "1234567890",
      "email" -> "test@test.com",
      "letterToThisAddress" -> true
    )

    val completeModel = ContactingYouForm("1234567890", "test@test.com", true)

    "serialize as expected" in {
      Json.toJson(completeModel) must be(completeJson)
    }

    "deserialize as expected" in {
      completeJson.as[ContactingYouForm] must be(completeModel)
    }

    "write correct data" in {
      val model = ContactingYou("1234567890", "test@test.com")
      ContactingYou.formWrites.writes(model) must
        be(Map(
          "phoneNumber" -> Seq("1234567890"),
          "email" -> Seq("test@test.com")
        ))
    }

  }
}