/*
 * Copyright 2019 HM Revenue & Customs
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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.declaration.release7.RoleWithinBusinessRelease7
import play.api.libs.json.{JsSuccess, Json}
import utils.AmlsSpec

class AddPersonRelease7Spec extends AmlsSpec {

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

      "firstname contain invalid character" in {
        val formNames = Map(
          "firstName" -> Seq("Abe>>"),
          "lastName" -> Seq("Lincoln"),
          "positions" -> Seq("01")
        )

        AddPerson.formRule.validate(formNames) must be(
          Invalid(
            Seq(
              (Path \ "firstName") -> Seq(ValidationError("error.invalid.firstname.validation"))
            )
          )
        )
      }

      "middlename contain invalid character" in {
        val formNames = Map(
          "firstName" -> Seq("Abe"),
          "middleName" -> Seq(">>"),
          "lastName" -> Seq("Lincoln"),
          "positions" -> Seq("01")
        )

        AddPerson.formRule.validate(formNames) must be(
          Invalid(
            Seq(
              (Path \ "middleName") -> Seq(ValidationError("error.invalid.middlename.validation"))
            )
          )
        )
      }

      "lastname contain invalid character" in {
        val formNames = Map(
          "firstName" -> Seq("Abe"),
          "middleName" -> Seq("W"),
          "lastName" -> Seq(">>"),
          "positions" -> Seq("01")
        )

        AddPerson.formRule.validate(formNames) must be(
          Invalid(
            Seq(
              (Path \ "lastName") -> Seq(ValidationError("error.invalid.lastname.validation"))
            )
          )
        )
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
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.firstname.length")),
            (Path \ "middleName") -> Seq(ValidationError("error.invalid.middlename.length")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.lastname.length"))
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
