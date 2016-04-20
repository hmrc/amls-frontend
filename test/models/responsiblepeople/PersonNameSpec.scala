package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class PersonNameSpec extends PlaySpec with MockitoSugar {

  "Validate definitions" must {

    "successfully validate the first name" in {
      PersonName.firstNameType.validate("John") must be(Success("John"))
    }

    "fail validation if the first name is not provided" in {
      PersonName.firstNameType.validate("") must be(Failure(Seq(Path -> Seq(ValidationError("error.required.firstname")))))
    }

    "fail validation if the first name is more than 35 characters" in {
      PersonName.firstNameType.validate("JohnJohnJohnJohnJohnJohnJohnJohnJohnJohn") must
        be(Failure(Seq(Path -> Seq(ValidationError("error.invalid.length.firstname")))))
    }


    "fail validation if the middle name is more than 35 characters" in {
      PersonName.middleNameType.validate("EnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvy") must
        be(Failure(Seq(Path -> Seq(ValidationError("error.invalid.length.middlename")))))
    }

    "successfully validate the last name" in {
      PersonName.lastNameType.validate("Doe") must be(Success("Doe"))
    }

    "fail validation if the last name is not provided" in {
      PersonName.lastNameType.validate("") must be(Failure(Seq(Path -> Seq(ValidationError("error.required.lastname")))))
    }

    "fail validation if the last name is more than 35 characters" in {
      PersonName.lastNameType.validate("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe") must
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
      PersonName.formRule.validate(urlFormEncoded) must be(Success(PersonName("John", Some("Envy"), "Doe", IsKnownByOtherNamesNo)))
    }

    "successfully validate given the middle name is optional" in {
      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "lastName" -> Seq("Doe"),
        "isKnownByOtherNames" -> Seq("false")

      )
      PersonName.formRule.validate(urlFormEncoded) must be(Success(PersonName("John", None, "Doe", IsKnownByOtherNamesNo)))
    }


    "fail validation when fields are missing" in {

      PersonName.formRule.validate(Map.empty) must
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

      PersonName.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when last name is missing" in {

      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "isKnownByOtherNames" -> Seq("false")
      )

      PersonName.formRule.validate(urlFormEncoded) must
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

      PersonName.formRule.validate(urlFormEncoded) must
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
        "isKnownByOtherNames" -> false
      )

      PersonName.jsonReads.reads(json) must be(JsSuccess(PersonName("John", Some("Envy"), "Doe", IsKnownByOtherNamesNo)))
    }

    "Write the json successfully from the AddPerson domain object created" in {

      val addPerson = PersonName("John", Some("Envy"), "Doe", IsKnownByOtherNamesNo)

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "isKnownByOtherNames" -> false
      )

      PersonName.jsonWrites.writes(addPerson) must be(json)
    }
  }

}
