package models.responsiblepeople

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.MutableDataStructures"))
class PersonNameSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {

        val data = Map(
          "firstName" -> Seq("firstName"),
          "middleName" -> Seq("middleName"),
          "lastName" -> Seq("lastName"),
          "hasPreviousName" -> Seq("true"),
          "previous.firstName" -> Seq("previousFirstName"),
          "previous.middleName" -> Seq("previousMiddleName"),
          "previous.lastName" -> Seq("previousLastName"),
          "previous.date.year" -> Seq("1990"),
          "previous.date.month" -> Seq("02"),
          "previous.date.day" -> Seq("24"),
          "hasOtherNames" -> Seq("true"),
          "otherNames" -> Seq("Doc")
        )

        val validPerson = PersonName(
          firstName = "firstName",
          middleName = Some("middleName"),
          lastName = "lastName",
          previousName = Some(
            PreviousName(
              firstName = Some("previousFirstName"),
              middleName = Some("previousMiddleName"),
              lastName = Some("previousLastName"),
              // scalastyle:off magic.number
              date = new LocalDate(1990, 2, 24)
            )
          ),
          otherNames = Some("Doc")
        )

        PersonName.formRule.validate(data) must equal(Valid(validPerson))
      }

      "the middle name is optional and previous/other names are not required" in {

        val data = Map(
          "firstName" -> Seq("firstName"),
          "lastName" -> Seq("lastName"),
          "hasPreviousName" -> Seq("false"),
          "hasOtherNames" -> Seq("false")
        )

        val validPerson = PersonName(
          firstName = "firstName",
          middleName = None,
          lastName = "lastName",
          previousName = None,
          otherNames = None
        )

        PersonName.formRule.validate(data) must equal(Valid(validPerson))
      }
    }

    "fail validation" when {

      "required fields are missing" when {
        "nothing has been selected" in {

          PersonName.formRule.validate(Map(
            "firstName" -> Seq(""),
            "lastName" -> Seq(""),
            "hasPreviousName" -> Seq(""),
            "hasOtherNames" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required.rp.first_name")),
            (Path \ "lastName") -> Seq(ValidationError("error.required.rp.last_name")),
            (Path \ "hasPreviousName") -> Seq(ValidationError("error.required.rp.hasPreviousName")),
            (Path \ "hasOtherNames") -> Seq(ValidationError("error.required.rp.hasOtherNames"))
          )))
        }

        "fields have been selected" in {

          PersonName.formRule.validate(Map(
            "firstName" -> Seq("firstName"),
            "lastName" -> Seq("lastName"),
            "hasPreviousName" -> Seq("true"),
            "hasOtherNames" -> Seq("true"),
            "previous.date.year" -> Seq(""),
            "previous.date.month" -> Seq(""),
            "previous.date.day" -> Seq(""),
            "previous.firstName" -> Seq(""),
            "previous.middleName" -> Seq(""),
            "previous.lastName" -> Seq(""),
            "otherNames" -> Seq("")
          )) must
            equal(Invalid(Seq(
              (Path \ "previous") -> Seq(ValidationError("error.rp.previous.invalid")),
              (Path \ "previous" \ "date") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")),
              (Path \ "otherNames") -> Seq(ValidationError("error.required.rp.otherNames"))
            )))
        }
      }

      "input length is too great" in {

        val data = Map(
          "firstName" -> Seq("firstName" * 36),
          "middleName" -> Seq("middleName" * 36),
          "lastName" -> Seq("lastName" * 36),
          "hasPreviousName" -> Seq("true"),
          "previous.firstName" -> Seq("previousFirstName" * 36),
          "previous.middleName" -> Seq("previousMiddleName" * 36),
          "previous.lastName" -> Seq("previousLastName" * 36),
          "hasOtherNames" -> Seq("true"),
          "otherNames" -> Seq("D" * 141),
          "previous.date.year" -> Seq("1990"),
          "previous.date.month" -> Seq("2"),
          "previous.date.day" -> Seq("24")
        )

        PersonName.formRule.validate(data) must
          equal(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "previous" \ "firstName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "previous" \ "middleName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "previous" \ "lastName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "otherNames") -> Seq(ValidationError("error.invalid.maxlength.140"))
          )))
      }

      "date is invalid" in {

        val data = Map(
          "firstName" -> Seq("firstName"),
          "middleName" -> Seq("middleName"),
          "lastName" -> Seq("lastName"),
          "hasPreviousName" -> Seq("true"),
          "previous.firstName" -> Seq("previousFirstName"),
          "previous.middleName" -> Seq("previousMiddleName"),
          "previous.lastName" -> Seq("previousLastName"),
          "previous.date.year" -> Seq("199000"),
          "previous.date.month" -> Seq("02"),
          "previous.date.day" -> Seq("24"),
          "hasOtherNames" -> Seq("true"),
          "otherNames" -> Seq("Doc")
        )

        PersonName.formRule.validate(data) must equal(Invalid(Seq(
          (Path \ "previous" \ "date") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
        )))
      }

      "fields have invalid characters" in {

        PersonName.formRule.validate(Map(
          "firstName" -> Seq("92)(OELer"),
          "middleName" -> Seq("£*($*)(ERKLFD "),
          "lastName" -> Seq("9*£@$"),
          "hasPreviousName" -> Seq("true"),
          "hasOtherNames" -> Seq("true"),
          "previous.date.year" -> Seq("2005"),
          "previous.date.month" -> Seq("1"),
          "previous.date.day" -> Seq("1"),
          "previous.firstName" -> Seq("($£*£$"),
          "previous.middleName" -> Seq(")£(@$)$( "),
          "previous.lastName" -> Seq("$&£@$*&$%&$"),
          "otherNames" -> Seq("ABC{}ABC")
        )) must
          equal(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "previous" \ "firstName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "previous" \ "middleName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "previous" \ "lastName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "otherNames") -> Seq(ValidationError("err.text.validation"))
          )))
      }

    }

  }

  "fullName" must {
    "return a correctly formatted name" in {
      PersonName("firstName", Some("middleName"), "lastName", None, None).fullName must be("firstName middleName lastName")
      PersonName("firstName", None, "lastName", None, None).fullName must be("firstName lastName")
    }
  }
}
