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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class ContactDetailsSpec extends PlaySpec with MockitoSugar {

    "Form Rules and Writes" must {

      "successfully validate given all fields" in {

        val urlFormEncoded = Map(
          "phoneNumber" -> Seq("07000000000"),
          "emailAddress" -> Seq("myname@example.com")
        )

        ContactDetails.formReads.validate(urlFormEncoded) must be(Valid(ContactDetails("07000000000", "myname@example.com")))
      }

      "fail to validate" when {

        "no option is selected" in {

          val emptyForm = Map(
            "phoneNumber" -> Seq(""),
            "emailAddress" -> Seq("")
          )

          ContactDetails.formReads.validate(emptyForm) must
            be(Invalid(Seq(
              (Path \ "phoneNumber") -> Seq(ValidationError("error.required.phone.number")),
              (Path \ "emailAddress") -> Seq(ValidationError("error.required.rp.email"))
            )))
        }

        "phone number is missing" in {

          val urlFormEncoded = Map(
            "phoneNumber" -> Seq(""),
            "emailAddress" -> Seq("myname@example.com")
          )

          ContactDetails.formReads.validate(urlFormEncoded) must
            be(Invalid(Seq(
              (Path \ "phoneNumber") -> Seq(ValidationError("error.required.phone.number"))
            )))
        }

        "email is missing" in {

          val urlFormEncoded = Map(
            "phoneNumber" -> Seq("07000000000"),
            "emailAddress" -> Seq("")
          )

          ContactDetails.formReads.validate(urlFormEncoded) must
            be(Invalid(Seq(
              (Path \ "emailAddress") -> Seq(ValidationError("error.required.rp.email"))
            )))
        }

        "email is invalid" in {

          val urlFormEncoded = Map(
            "phoneNumber" -> Seq("07000000000"),
            "emailAddress" -> Seq("invalid-email.com")
          )
          ContactDetails.formReads.validate(urlFormEncoded) must
            be(Invalid(Seq(
              (Path \ "emailAddress") -> Seq(ValidationError("error.invalid.rp.email"))
            )))
        }

        "phone number is invalid" in {

          val urlFormEncoded = Map(
            "phoneNumber" -> Seq("invalid phone"),
            "emailAddress" -> Seq("myname@example.com")
          )
          ContactDetails.formReads.validate(urlFormEncoded) must
            be(Invalid(Seq(
              (Path \ "phoneNumber") -> Seq(ValidationError("err.invalid.phone.number"))
            )))
        }

        "email is too long" in {
          val urlFormEncoded = Map(
            "phoneNumber" -> Seq("07000000000"),
            "emailAddress" -> Seq("email.com" * 100)
          )
          ContactDetails.formReads.validate(urlFormEncoded) must
            be(Invalid(Seq(
              (Path \ "emailAddress") -> Seq(ValidationError("error.max.length.rp.email"))
            )))
        }

        "phone number is too long" in {
          val urlFormEncoded = Map(
            "phoneNumber" -> Seq("0987654" * 30),
            "emailAddress" -> Seq("myname@example.com")
          )
          ContactDetails.formReads.validate(urlFormEncoded) must
            be(Invalid(Seq(
              (Path \ "phoneNumber") -> Seq(ValidationError("error.max.length.phone"))
            )))
        }

      }

    }

  "JSON Read/Write " must {

      "Read the json and return the InKnownByOtherNamesYes domain object successfully" in {

        val json = Json.obj(
          "phoneNumber" -> "07000000000",
          "emailAddress" -> "myname@example.com"
        )

        ContactDetails.formats.reads(json) must
          be(JsSuccess(ContactDetails("07000000000", "myname@example.com")))
      }

      "Write the json successfully from the InKnownByOtherNamesYes domain object created" in {

        val contactDetails = ContactDetails("07000000000", "myname@example.com")

        val json = Json.obj(
          "phoneNumber" -> "07000000000",
          "emailAddress" -> "myname@example.com"
        )

        ContactDetails.formats.writes(contactDetails) must be(json)
      }
    }

}
