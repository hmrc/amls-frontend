/*
 * Copyright 2024 HM Revenue & Customs
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

import models.declaration.release7.RoleWithinBusinessRelease7
import play.api.libs.json.{JsSuccess, Json}
import utils.AmlsSpec

class AddPersonRelease7Spec extends AmlsSpec {

  "JSON" must {

    "Read correctly from JSON when the MiddleName is missing (preRelease7 json data)" in {
      val json = Json.obj(
        "firstName"          -> "FNAME",
        "lastName"           -> "LNAME",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(
        JsSuccess(
          AddPerson("FNAME", None, "LNAME", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))
        )
      )
    }

    "Read correctly from JSON when the MiddleName is missing " in {
      val json = Json.obj(
        "firstName"          -> "FNAME",
        "lastName"           -> "LNAME",
        "roleWithinBusiness" -> Set("Director")
      )

      AddPerson.jsonReads.reads(json) must be(
        JsSuccess(
          AddPerson("FNAME", None, "LNAME", RoleWithinBusinessRelease7(Set(models.declaration.release7.Director)))
        )
      )
    }

    "Read the json and return the AddPerson domain object successfully (preRelease7 json data)" in {

      val json = Json.obj(
        "firstName"          -> "first",
        "middleName"         -> "middle",
        "lastName"           -> "last",
        "roleWithinBusiness" -> "02"
      )

      AddPerson.jsonReads.reads(json) must be(
        JsSuccess(
          AddPerson(
            "first",
            Some("middle"),
            "last",
            RoleWithinBusinessRelease7(Set(models.declaration.release7.Director))
          )
        )
      )
    }

    "Read the json and return the AddPerson domain object successfully " in {

      val json = Json.obj(
        "firstName"          -> "first",
        "middleName"         -> "middle",
        "lastName"           -> "last",
        "roleWithinBusiness" -> Seq("Director", "Partner", "SoleProprietor")
      )

      AddPerson.jsonReads.reads(json) must be(
        JsSuccess(
          AddPerson(
            "first",
            Some("middle"),
            "last",
            RoleWithinBusinessRelease7(
              Set(
                models.declaration.release7.Director,
                models.declaration.release7.Partner,
                models.declaration.release7.SoleProprietor
              )
            )
          )
        )
      )
    }

    "Write the json successfully from the AddPerson domain object created" in {

      val addPerson = AddPerson(
        "first",
        Some("middle"),
        "last",
        RoleWithinBusinessRelease7(
          Set(
            models.declaration.release7.Director,
            models.declaration.release7.Partner,
            models.declaration.release7.SoleProprietor
          )
        )
      )

      val json = Json.obj(
        "firstName"          -> "first",
        "middleName"         -> "middle",
        "lastName"           -> "last",
        "roleWithinBusiness" -> Seq("Director", "Partner", "SoleProprietor")
      )

      AddPerson.jsonWrites.writes(addPerson) must be(json)
    }
  }
}
