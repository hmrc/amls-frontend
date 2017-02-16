package models.responsiblepeople

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.MutableDataStructures"))
class PersonNameSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate given all fields" in {

      val data = Map(
        "firstName" -> Seq("John"),
        "middleName" -> Seq("Envy"),
        "lastName" -> Seq("Doe"),
        "hasPreviousName" -> Seq("true"),
        "previous.firstName" -> Seq("Marty"),
        "previous.middleName" -> Seq("Mc"),
        "previous.lastName" -> Seq("Fly"),
        "previous.date.year" -> Seq("1990"),
        "previous.date.month" -> Seq("02"),
        "previous.date.day" -> Seq("24"),
        "hasOtherNames" -> Seq("true"),
        "otherNames" -> Seq("Doc")
      )

      PersonName.formRule.validate(data) must
        equal(Valid(
          PersonName(
            firstName = "John",
            middleName = Some("Envy"),
            lastName = "Doe",
            previousName = Some(
              PreviousName(
                firstName = Some("Marty"),
                middleName = Some("Mc"),
                lastName = Some("Fly"),
                // scalastyle:off magic.number
                date = new LocalDate(1990, 2, 24)
              )
            ),
            otherNames = Some("Doc")
          )
        ))
    }

    "successfully validate given the middle name is optional and previous/other names are not required" in {

      val data = Map(
        "firstName" -> Seq("John"),
        "lastName" -> Seq("Doe"),
        "hasPreviousName" -> Seq("false"),
        "hasOtherNames" -> Seq("false")
      )

      PersonName.formRule.validate(data) must
        equal(Valid(
          PersonName(
            firstName = "John",
            middleName = None,
            lastName = "Doe",
            previousName = None,
            otherNames = None
          )
        ))
    }

    "fail validation when fields are missing (minimal)" in {

      PersonName.formRule.validate(Map(
        "firstName" -> Seq(""),
        "lastName" -> Seq(""),
        "hasPreviousName" -> Seq(""),
        "hasOtherNames" -> Seq("")
      )) must
        equal(Invalid(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required.rp.first_name")),
          (Path \ "lastName") -> Seq(ValidationError("error.required.rp.last_name")),
          (Path \ "hasPreviousName") -> Seq(ValidationError("error.required.rp.hasPreviousName")),
          (Path \ "hasOtherNames") -> Seq(ValidationError("error.required.rp.hasOtherNames"))
        )))

    }

    "fail validation when fields are missing (full)" in {

      PersonName.formRule.validate(Map(
        "firstName" -> Seq(""),
        "lastName" -> Seq(""),
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
          (Path \ "firstName") -> Seq(ValidationError("error.required.rp.first_name")),
          (Path \ "lastName") -> Seq(ValidationError("error.required.rp.last_name")),
          (Path \ "previous") -> Seq(ValidationError("error.rp.previous.invalid")),
          (Path \ "previous" \ "date") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")),
          (Path \ "otherNames") -> Seq(ValidationError("error.required.rp.otherNames"))
        )))
    }

    "fail to validate because of input length" in {

      val data = Map(
        "firstName" -> Seq("John" * 36),
        "middleName" -> Seq("John" * 36),
        "lastName" -> Seq("Doe" * 36),
        "hasPreviousName" -> Seq("true"),
        "previous.firstName" -> Seq("Marty" * 36),
        "previous.middleName" -> Seq("Mc" * 36),
        "previous.lastName" -> Seq("Fly" * 36),
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
          (Path \ "otherNames") -> Seq(ValidationError("error.invalid.length.otherNames"))
        )))
    }

    "fail validation when fields have invalid characters (minimal)" in {

      PersonName.formRule.validate(Map(
        "firstName" -> Seq("(£*$"),
        "middleName" -> Seq("£*$(£IOKd"),
        "lastName" -> Seq("(£$ *(£ $"),
        "hasPreviousName" -> Seq("false"),
        "hasOtherNames" -> Seq("false")
      )) must
        equal(Invalid(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.validation")),
          (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.validation")),
          (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.validation"))
        )))

    }

    "fail validation when fields have invalid characters (full)" in {

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
        "otherNames" -> Seq("false")
      )) must
        equal(Invalid(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.validation")),
          (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.validation")),
          (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.validation")),
          (Path \ "previous" \ "firstName") -> Seq(ValidationError("error.invalid.common_name.validation")),
          (Path \ "previous" \ "middleName") -> Seq(ValidationError("error.invalid.common_name.validation")),
          (Path \ "previous" \ "lastName") -> Seq(ValidationError("error.invalid.common_name.validation"))
        )))
    }
  }

  "fullName" must {

    "return a correctly formatted name" in {
      PersonName("John", Some("Paul"), "Smith", None, None).fullName must be ("John Paul Smith")
      PersonName("John", None, "Smith", None, None).fullName must be ("John Smith")
    }
  }
}
