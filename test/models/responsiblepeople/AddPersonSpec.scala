package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class AddPersonSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate given all fields" in {
      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "middleName" -> Seq("Envy"),
        "lastName" -> Seq("Doe")
      )
      AddPerson.formRule.validate(urlFormEncoded) must be(Success(AddPerson("John", Some("Envy"), "Doe")))
    }

    "successfully validate given the middle name is optional" in {
      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "lastName" -> Seq("Doe")
      )
      AddPerson.formRule.validate(urlFormEncoded) must be(Success(AddPerson("John", None, "Doe")))
    }


    "fail validation when fields are missing" in {

      AddPerson.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required")),
          (Path \ "lastName") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when first name is missing" in {

      val urlFormEncoded = Map(
        "lastName" -> Seq("Doe"))

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when last name is missing" in {

      val urlFormEncoded = Map(
        "firstName" -> Seq("John")
      )

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "lastName") -> Seq(ValidationError("error.required"))
        )))
    }


    "fail to validate when firstName or lastName are more than the required length" in {

      val urlFormEncoded = Map(
        "firstName" -> Seq("JohnJohnJohnJohnJohnJohnJohnJohnJohn"),
        "lastName" -> Seq("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe")
      )

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.maxLength", 35)),
          (Path \ "lastName") -> Seq(ValidationError("error.maxLength", 35))
        )))
    }
  }

  "JSON" must {

    "Read the json and return the AddPerson domain object successfully" in {

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("John", Some("Envy"), "Doe")))
    }

    "Write the json successfully from the AddPerson domain object created" in {

      val addPerson = AddPerson("John", Some("Envy"), "Doe")

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe"
      )

      AddPerson.jsonWrites.writes(addPerson) must be(json)
    }
  }

}
