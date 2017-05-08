package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class IsKnownByOtherNamesSpec extends PlaySpec with MockitoSugar {

  "Validate definitions" must {

    "successfully validate the name" in {
      IsKnownByOtherNames.otherFirstNameType.validate("firstName") must be(Valid("firstName"))
    }

    "pass validation if the middle name is not provided" in {
      IsKnownByOtherNames.otherMiddleNameType.validate("") must be(Valid(""))
    }

    "fail validation if the name is more than 35 characters" in {
      IsKnownByOtherNames.otherLastNameType.validate("firstNamefirstNamefirstNamefirstNamefirstNamefirstNamefirstName") must
        be(Invalid(Seq(Path -> Seq(ValidationError("error.invalid.length.lastname")))))
    }

  }

  "Form Rules and Writes" must {

    "successfully validate given all fields" in {
      val urlFormEncoded = Map(
        "isKnownByOtherNames" -> Seq("true"),
        "otherfirstnames" -> Seq("firstName"),
        "othermiddlenames" -> Seq("middleName"),
        "otherlastnames" -> Seq("lastName")
      )
      IsKnownByOtherNames.formRule.validate(urlFormEncoded) must be(Valid(IsKnownByOtherNamesYes("firstName", Some("middleName"), "lastName")))
    }

    "successfully validate given the middle name is optional" in {
      val urlFormEncoded = Map(
        "isKnownByOtherNames" -> Seq("true"),
        "otherfirstnames" -> Seq("firstName"),
        "otherlastnames" -> Seq("lastName")
      )
      IsKnownByOtherNames.formRule.validate(urlFormEncoded) must be(Valid(IsKnownByOtherNamesYes("firstName", None, "lastName")))
    }


    "fail validation when no option is selected for isKnownByOtherNames" in {

      IsKnownByOtherNames.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "isKnownByOtherNames") -> Seq(ValidationError("error.required.rp.isknownbyothernames"))
        )))
    }

    "fail validation when isKnownByOtherNames is selcted but name not provided" in {

      val urlFormEncoded = Map(
        "isKnownByOtherNames" -> Seq("true")
      )

      IsKnownByOtherNames.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "otherfirstnames") -> Seq(ValidationError("error.required")),
          (Path \ "otherlastnames") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when first name is missing" in {

      val urlFormEncoded = Map(
        "isKnownByOtherNames" -> Seq("true"),
        "otherlastnames" -> Seq("lastName")
      )

      IsKnownByOtherNames.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "otherfirstnames") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate when last name is missing" in {

      val urlFormEncoded = Map(
        "isKnownByOtherNames" -> Seq("true"),
        "otherfirstnames" -> Seq("firstName")
      )

      IsKnownByOtherNames.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "otherlastnames") -> Seq(ValidationError("error.required"))
        )))
    }


    "fail to validate when otherfirstnames or otherlastnames are more than the required length" in {

      val urlFormEncoded = Map(
        "isKnownByOtherNames" -> Seq("true"),
        "otherfirstnames" -> Seq("firstNamefirstNamefirstNamefirstNamefirstNamefirstNamefirstName"),
        "otherlastnames" -> Seq("lastNamelastNamelastNamelastNamelastNamelastNamelastNamelastNamelastNamelastName")
      )

      IsKnownByOtherNames.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "otherfirstnames") -> Seq(ValidationError("error.invalid.length.firstname")),
          (Path \ "otherlastnames") -> Seq(ValidationError("error.invalid.length.lastname"))
        )))
    }
  }

  "JSON Read/Write " must {

    "Read the json and return the InKnownByOtherNamesYes domain object successfully" in {

      val json = Json.obj(
        "isKnownByOtherNames" -> true,
        "otherfirstnames" -> "firstName",
        "othermiddlenames" -> "middleName",
        "otherlastnames" -> "lastName"
      )

      IsKnownByOtherNames.jsonReads.reads(json) must
        be(JsSuccess(IsKnownByOtherNamesYes("firstName", Some("middleName"), "lastName"), JsPath ))
    }

    "Write the json successfully from the InKnownByOtherNamesYes domain object created" in {

      val isKnownByOtherNamesYes = IsKnownByOtherNamesYes("firstName", Some("middleName"), "lastName")

      val json = Json.obj(
        "isKnownByOtherNames" -> true,
        "otherfirstnames" -> "firstName",
        "othermiddlenames" -> "middleName",
        "otherlastnames" -> "lastName"
      )

      IsKnownByOtherNames.jsonWrites.writes(isKnownByOtherNamesYes) must be(json)
    }
  }

}
