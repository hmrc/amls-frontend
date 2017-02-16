package models.declaration

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import models.declaration.release7.RoleWithinBusinessRelease7
import play.api.libs.json.{JsSuccess, Json}
import play.api.test.FakeApplication

class AddPersonSpec extends PlaySpec with MockitoSugar with OneAppPerSuite{

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> false))

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields (preRelease7)" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "middleName" -> Seq("Envy"),
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("01")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Valid(AddPerson("John", Some("Envy"), "Doe",
          models.declaration.release7.RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)))))
      }

      "a middle name is not provided (preRelease7)" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("01")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Valid(AddPerson("John", None, "Doe",
          models.declaration.release7.RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)))))
      }
    }

    "fail validation" when {
      "fields are missing represented by an empty Map" in {

        AddPerson.formRule.validate(Map.empty) must
          be(Invalid(Seq(
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
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required.declaration.first_name")),
            (Path \ "lastName") -> Seq(ValidationError("error.required.declaration.last_name")),
            (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))
          )))
      }

      "first name is missing (preRelease7)" in {

        val urlFormEncoded = Map(
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required"))
          )))
      }

      "last name is missing (preRelease7)" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "lastName") -> Seq(ValidationError("error.required"))
          )))
      }

      "role within business is missing" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "lastName" -> Seq("Doe")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.required"))
          )))
      }

      "firstName or lastName are more than the required length (preRelease7)" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("a" * 36),
          "lastName" -> Seq("b" * 36),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.length.firstname")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.length.lastname"))
          )))
      }

    }
  }

  "JSON" must {

    "Read correctly from JSON when the MiddleName is missing (preRelease7)" in {
      val json = Json.obj(
        "firstName" -> "FNAME",
        "lastName" -> "LNAME",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("FNAME", None, "LNAME",
        models.declaration.release7.RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))))
    }


    "Read the json and return the AddPerson domain object successfully (preRelease7)" in {

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("John", Some("Envy"), "Doe", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))))
    }

    "Write the json successfully from the AddPerson domain object created (preRelease7)" in {

      val addPerson = AddPerson("John", Some("Envy"), "Doe", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "roleWithinBusiness" -> Json.arr("Director")
      )

      AddPerson.jsonWrites.writes(addPerson) must be(json)
    }
  }


}

class AddPersonRelease7Spec extends PlaySpec with MockitoSugar with OneAppPerSuite{

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> true))

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "middleName" -> Seq("Envy"),
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("BeneficialShareholder")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Valid(AddPerson("John", Some("Envy"), "Doe", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)))))
      }

      "a middle name is not provided (preRelease7)" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("BeneficialShareholder")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Valid(AddPerson("John", None, "Doe", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)))))
      }
    }

    "fail validation" when {
      "fields are missing represented by an empty Map" in {

        AddPerson.formRule.validate(Map.empty) must
          be(Invalid(Seq(
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
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required.declaration.first_name")),
            (Path \ "lastName") -> Seq(ValidationError("error.required.declaration.last_name")),
            (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))
          )))
      }

      "first name is missing" in {

        val urlFormEncoded = Map(
          "lastName" -> Seq("Doe"),
          "roleWithinBusiness" -> Seq("BeneficialShareholder")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required"))
          )))
      }

      "last name is missing" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "roleWithinBusiness" -> Seq("BeneficialShareholder")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "lastName") -> Seq(ValidationError("error.required"))
          )))
      }

      "role within business is missing" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("John"),
          "lastName" -> Seq("Doe")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.required"))
          )))
      }

      "firstName or lastName are more than the required length" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("a" * 36),
          "lastName" -> Seq("b" * 36),
          "roleWithinBusiness" -> Seq("BeneficialShareholder")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.length.firstname")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.length.lastname"))
          )))
      }
    }
  }

  "JSON" must {

    "Read correctly from JSON when the MiddleName is missing (preRelease7 json data)" in {
      val json = Json.obj(
        "firstName" -> "FNAME",
        "lastName" -> "LNAME",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("FNAME", None, "LNAME", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))))
    }

    "Read correctly from JSON when the MiddleName is missing " in {
      val json = Json.obj(
        "firstName" -> "FNAME",
        "lastName" -> "LNAME",
        "roleWithinBusiness" -> Set("Director")
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("FNAME", None, "LNAME", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))))
    }


    "Read the json and return the AddPerson domain object successfully (preRelease7 json data)" in {

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("John", Some("Envy"), "Doe", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))))
    }

    "Read the json and return the AddPerson domain object successfully " in {

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "roleWithinBusiness" -> Seq("Director","Partner","SoleProprietor")
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("John", Some("Envy"), "Doe", RoleWithinBusinessRelease7(Set(
        models.declaration.release7.Director,
        models.declaration.release7.Partner,
        models.declaration.release7.SoleProprietor)))))
    }



    "Write the json successfully from the AddPerson domain object created" in {

      val addPerson = AddPerson("John", Some("Envy"), "Doe", RoleWithinBusinessRelease7(Set(
        models.declaration.release7.Director,
        models.declaration.release7.Partner,
        models.declaration.release7.SoleProprietor)))

      val json = Json.obj(
        "firstName" -> "John",
        "middleName" -> "Envy",
        "lastName" -> "Doe",
        "roleWithinBusiness" -> Seq("Director","Partner","SoleProprietor")
      )

      AddPerson.jsonWrites.writes(addPerson) must be(json)
    }
  }


}
