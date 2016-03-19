package models.declaration

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError

class AddPersonSpec extends PlaySpec with MockitoSugar {

  "When the user posts the input it" must {

    "successfully validate given all fields" in {
      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "middleName" -> Seq("Envy"),
        "lastName" -> Seq("Doe"),
        "roleWithinBusiness" -> Seq("01")
      )
      AddPerson.formRule.validate(urlFormEncoded) must be(Success(AddPerson("John", Some("Envy"), "Doe", BeneficialShareholder)))
    }

    "successfully validate given the middle name is optional" in {
      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "lastName" -> Seq("Doe"),
        "roleWithinBusiness" -> Seq("01")
      )
      AddPerson.formRule.validate(urlFormEncoded) must be(Success(AddPerson("John", None, "Doe", BeneficialShareholder)))
    }


    "fail to validate when fields are missing" in {

      AddPerson.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required")),
          (Path \ "lastName") -> Seq(ValidationError("error.required")),
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when first name is missing" in {

      val urlFormEncoded = Map(
        "lastName" -> Seq("Doe"),
        "roleWithinBusiness" -> Seq("01")
      )

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when last name is missing" in {

      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "roleWithinBusiness" -> Seq("01")
      )

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "lastName") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when role within business is missing" in {

      val urlFormEncoded = Map(
        "firstName" -> Seq("John"),
        "lastName" -> Seq("Doe")
      )

      AddPerson.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when firstName or lastName are more than the required length" in {

      val urlFormEncoded = Map(
        "firstName" -> Seq("JohnJohnJohnJohnJohnJohnJohnJohnJohn"),
        "lastName" -> Seq("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe"),
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
