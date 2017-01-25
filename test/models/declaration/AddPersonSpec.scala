package models.declaration

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class AddPersonSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "middleName" -> Seq("Envy"),
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("01")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Success(AddPerson("John", Some("Envy"), "Doe", BeneficialShareholder)))
      }

      "a middle name is not provided" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("01")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Success(AddPerson("John", None, "Doe", BeneficialShareholder)))
      }
    }

    "fail validation" when {
      "fields are missing represented by an empty Map" in {

        AddPerson.formRule.validate(Map.empty) must
          be(Failure(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required")),
            (Path \ "lastName") -> Seq(ValidationError("error.required")),
            (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.required"))
          )))
      }

      "fields are missing represented by empty strings" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq(""),
          "middleName" -> Seq(""),
          "lastName" -> Seq(""),
          "roleWithinBusiness" -> Seq("")
        )
        AddPerson.formRule.validate(urlFormEncoded) must
          be(Failure(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required")),
            (Path \ "lastName") -> Seq(ValidationError("error.required")),
            (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))
          )))
      }

      "first name is missing" in {

        val urlFormEncoded = Map(
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Failure(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required"))
          )))
      }

      "last name is missing" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Failure(Seq(
            (Path \ "lastName") -> Seq(ValidationError("error.required"))
          )))
      }

      "role within business is missing" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "lastName" -> Seq("Doe")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Failure(Seq(
            (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.required"))
          )))
      }

      "firstName or lastName are more than the required length" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("a" * 36),
          "lastName" -> Seq("b" * 36),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Failure(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.maxLength", 35)),
            (Path \ "lastName") -> Seq(ValidationError("error.maxLength", 35))
          )))
      }
    }
  }

  "JSON" must {

    "Read correctly from JSON when the MiddleName is missing" in {
      val json = Json.obj(
        "firstName" -> "FNAME",
        "lastName" -> "LNAME",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("FNAME", None, "LNAME", Director)))
    }


    "Read the json and return the AddPerson domain object successfully" in {

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("John", Some("Envy"), "Doe", Director)))
    }

    "Write the json successfully from the AddPerson domain object created" in {

      val addPerson = AddPerson("John", Some("Envy"), "Doe", Director)

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonWrites.writes(addPerson) must be(json)
    }
  }


}
