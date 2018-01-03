/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.declaration

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import models.declaration.release7.RoleWithinBusinessRelease7
import play.api.libs.json.{JsSuccess, Json}
import play.api.test.FakeApplication

class AddPersonSpec extends PlaySpec with MockitoSugar with OneAppPerSuite{

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> false))

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields (preRelease7)" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("first"),
          "middleName" -> Seq("middle"),
          "lastName" -> Seq("last"),
          "roleWithinBusiness" -> Seq("01")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Valid(AddPerson("first", Some("middle"), "last",
          models.declaration.release7.RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)))))
      }

      "a middle name is not provided (preRelease7)" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("first"),
          "lastName" -> Seq("last"),
          "roleWithinBusiness" -> Seq("01")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Valid(AddPerson("first", None, "last",
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
          "lastName" -> Seq("last"),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required"))
          )))
      }

      "last name is missing (preRelease7)" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("first"),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "lastName") -> Seq(ValidationError("error.required"))
          )))
      }

      "role within business is missing" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("first"),
          "lastName" -> Seq("last")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.required"))
          )))
      }

      "firstName, middle name or lastName are more than the required length (preRelease7)" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("a" * 36),
          "lastName" -> Seq("b" * 36),
          "middleName" -> Seq("c" * 36),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.length"))
          )))
      }

      "firstName, middle name or lastName contain invalid characters" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("(SDF904)"),
          "lastName" -> Seq("$()%LKDf"),
          "middleName" -> Seq("89548594"),
          "roleWithinBusiness" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.validation"))
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
        "firstName" -> "first",
        "middleName" -> "middle",
        "lastName" -> "last",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("first", Some("middle"), "last", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))))
    }

    "Write the json successfully from the AddPerson domain object created (preRelease7)" in {

      val addPerson = AddPerson("first", Some("middle"), "last", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))

      val json = Json.obj(
        "firstName" -> "first",
        "middleName" -> "middle",
        "lastName" -> "last",
        "roleWithinBusiness" -> Json.arr("Director")
      )

      AddPerson.jsonWrites.writes(addPerson) must be(json)
    }
  }


}

class AddPersonRelease7Spec extends PlaySpec with MockitoSugar with OneAppPerSuite{

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true))

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("first"),
          "middleName" -> Seq("middle"),
          "lastName" -> Seq("last"),
          "positions" -> Seq("01")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Valid(AddPerson("first", Some("middle"), "last", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)))))
      }

      "a middle name is not provided (preRelease7)" in {
        val urlFormEncoded = Map(
          "firstName" -> Seq("first"),
          "lastName" -> Seq("last"),
          "positions" -> Seq("01")
        )
        AddPerson.formRule.validate(urlFormEncoded) must be(Valid(AddPerson("first", None, "last", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)))))
      }
    }

    "fail validation" when {
      "fields are missing represented by an empty Map" in {

        AddPerson.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required")),
            (Path \ "lastName") -> Seq(ValidationError("error.required")),
            (Path \ "positions") -> Seq(ValidationError("error.required"))
          )))
      }

      "fields are missing represented by empty strings" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq(""),
          "middleName" -> Seq(""),
          "lastName" -> Seq(""),
          "positions" -> Seq("")
        )
        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required.declaration.first_name")),
            (Path \ "lastName") -> Seq(ValidationError("error.required.declaration.last_name")),
            (Path \ "positions") -> Seq(ValidationError("error.invalid"))
          )))
      }

      "first name is missing" in {

        val urlFormEncoded = Map(
          "lastName" -> Seq("last"),
          "positions" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required"))
          )))
      }

      "last name is missing" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("first"),
          "positions" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "lastName") -> Seq(ValidationError("error.required"))
          )))
      }

      "role within business is missing" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("first"),
          "lastName" -> Seq("last")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "positions") -> Seq(ValidationError("error.required"))
          )))
      }

      "firstName, middle name or lastName are more than the required length" in {

        val urlFormEncoded = Map(
          "firstName" -> Seq("a" * 36),
          "lastName" -> Seq("b" * 36),
          "middleName" -> Seq("c" * 36),
          "positions" -> Seq("01")
        )

        AddPerson.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.length"))
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
        "firstName" -> "first",
        "middleName" -> "middle",
        "lastName" -> "last",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("first", Some("middle"), "last", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))))
    }

    "Read the json and return the AddPerson domain object successfully " in {

      val json = Json.obj(
        "firstName" -> "first",
        "middleName" -> "middle",
        "lastName" -> "last",
        "roleWithinBusiness" -> Seq("Director","Partner","SoleProprietor")
      )

      AddPerson.jsonReads.reads(json) must be(JsSuccess(AddPerson("first", Some("middle"), "last", RoleWithinBusinessRelease7(Set(
        models.declaration.release7.Director,
        models.declaration.release7.Partner,
        models.declaration.release7.SoleProprietor)))))
    }



    "Write the json successfully from the AddPerson domain object created" in {

      val addPerson = AddPerson("first", Some("middle"), "last", RoleWithinBusinessRelease7(Set(
        models.declaration.release7.Director,
        models.declaration.release7.Partner,
        models.declaration.release7.SoleProprietor)))

      val json = Json.obj(
        "firstName" -> "first",
        "middleName" -> "middle",
        "lastName" -> "last",
        "roleWithinBusiness" -> Seq("Director","Partner","SoleProprietor")
      )

      AddPerson.jsonWrites.writes(addPerson) must be(json)
    }
  }


}
