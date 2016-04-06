package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class AddPersonSpec extends PlaySpec with MockitoSugar {

  "Validate definitions" must {

    "successfully validate the first name" in {
      AddPerson.firstNameType.validate("John") must be(Success("John"))
    }

    "fail validation if the first name is not provided" in {
      AddPerson.firstNameType.validate("") must be(Failure(Seq(Path -> Seq(ValidationError("error.required.firstname")))))
    }

    "fail validation if the first name is more than 35 characters" in {
      AddPerson.firstNameType.validate("JohnJohnJohnJohnJohnJohnJohnJohnJohnJohn") must
        be(Failure(Seq(Path -> Seq(ValidationError("error.invalid.length.firstname")))))
    }


    "fail validation if the middle name is more than 35 characters" in {
      AddPerson.middleNameType.validate("EnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvy") must
        be(Failure(Seq(Path -> Seq(ValidationError("error.invalid.length.middlename")))))
    }

    "successfully validate the last name" in {
      AddPerson.lastNameType.validate("Doe") must be(Success("Doe"))
    }

    "fail validation if the last name is not provided" in {
      AddPerson.lastNameType.validate("") must be(Failure(Seq(Path -> Seq(ValidationError("error.required.lastname")))))
    }

    "fail validation if the last name is more than 35 characters" in {
      AddPerson.lastNameType.validate("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe") must
        be(Failure(Seq(Path -> Seq(ValidationError("error.invalid.length.lastname")))))
    }

  }

  "Form Rules and Writes" must {

    "successfully validate given all fields" in {
      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "middleName" -> Seq("Envy"),
        "lastName" -> Seq("Doe"),
        "isKnownByOtherNames" -> Seq("false")
      )
      AddPerson.formRule.validate(urlFormEncoded) must be(Success(AddPerson("John", Some("Envy"), "Doe", IsKnownByOtherNamesNo)))
    }

    "successfully validate given the middle name is optional" in {
      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "lastName" -> Seq("Doe"),
        "isKnownByOtherNames" -> Seq("false")

      )
      AddPerson.formRule.validate(urlFormEncoded) must be(Success(AddPerson("John", None, "Doe", IsKnownByOtherNamesNo)))
    }


    "fail validation when fields are missing" in {

      AddPerson.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required")),
          (Path \ "lastName") -> Seq(ValidationError("error.required")),
          (Path \ "isKnownByOtherNames") -> Seq(ValidationError("error.required.rp.isknownbyothernames"))
        )))
    }

    "fail to validate when first name is missing" in {

      val urlFormEncoded = Map(
        "lastName" -> Seq("Doe"),
        "isKnownByOtherNames" -> Seq("false")
      )

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when last name is missing" in {

      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "isKnownByOtherNames" -> Seq("false")
      )

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "lastName") -> Seq(ValidationError("error.required"))
        )))
    }


    "fail to validate when firstName or lastName are more than the required length" in {

      val urlFormEncoded = Map(
        "firstName" -> Seq("JohnJohnJohnJohnJohnJohnJohnJohnJohn"),
        "lastName" -> Seq("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe"),
        "isKnownByOtherNames" -> Seq("false")
      )

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.invalid.length.firstname")),
          (Path \ "lastName") -> Seq(ValidationError("error.invalid.length.lastname"))
        )))
    }
  }

  "JSON" must {

    "Read the json and return the AddPerson domain object successfully" in {

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "isKnownByOtherNames" -> "false"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("John", Some("Envy"), "Doe", IsKnownByOtherNamesNo)))
    }

    "Write the json successfully from the AddPerson domain object created" in {

      val addPerson = AddPerson("John", Some("Envy"), "Doe", IsKnownByOtherNamesNo)

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "isKnownByOtherNames" -> "false"
      )

      AddPerson.jsonWrites.writes(addPerson) must be(json)
    }
  }

}
