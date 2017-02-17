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
          "phoneNumber" -> Seq("07702755869"),
          "emailAddress" -> Seq("myname@example.com")
        )

        ContactDetails.formReads.validate(urlFormEncoded) must be(Valid(ContactDetails("07702755869", "myname@example.com")))
      }

      "fail validation when no option is selected" in {

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

      "fail to validate when phone number is missing" in {

        val urlFormEncoded = Map(
          "phoneNumber" -> Seq(""),
          "emailAddress" -> Seq("myname@example.com")
        )

        ContactDetails.formReads.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "phoneNumber") -> Seq(ValidationError("error.required.phone.number"))
          )))
      }

      "fail to validate when email is missing" in {

        val urlFormEncoded = Map(
          "phoneNumber" -> Seq("07702755869"),
          "emailAddress" -> Seq("")
        )

        ContactDetails.formReads.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "emailAddress") -> Seq(ValidationError("error.required.rp.email"))
          )))
      }

      "fail to validate when email is invalid" in {

        val urlFormEncoded = Map(
          "phoneNumber" -> Seq("07702755869"),
          "emailAddress" -> Seq("invalid-email.com")
        )
        ContactDetails.formReads.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "emailAddress") -> Seq(ValidationError("error.invalid.rp.email"))
          )))
      }

      "fail to validate when phone number is invalid" in {

        val urlFormEncoded = Map(
          "phoneNumber" -> Seq("invalid phone"),
          "emailAddress" -> Seq("myname@example.com")
        )
        ContactDetails.formReads.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "phoneNumber") -> Seq(ValidationError("err.invalid.phone.number"))
          )))
      }

    }

  "JSON Read/Write " must {

      "Read the json and return the InKnownByOtherNamesYes domain object successfully" in {

        val json = Json.obj(
          "phoneNumber" -> "07702755869",
          "emailAddress" -> "myname@example.com"
        )

        ContactDetails.formats.reads(json) must
          be(JsSuccess(ContactDetails("07702755869", "myname@example.com")))
      }

      "Write the json successfully from the InKnownByOtherNamesYes domain object created" in {

        val contactDetails = ContactDetails("07702755869", "myname@example.com")

        val json = Json.obj(
          "phoneNumber" -> "07702755869",
          "emailAddress" -> "myname@example.com"
        )

        ContactDetails.formats.writes(contactDetails) must be(json)
      }
    }

}
