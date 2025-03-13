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

package models.declaration.release7

import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import utils.AmlsSpec

class RoleWithinBusinessRelease7Spec extends AmlsSpec {

  "JSON validation" must {

    "successfully convert release 6 data to release 7 model" in {
      val json = Json.obj("roleWithinBusiness" -> "01")

      Json.fromJson[RoleWithinBusinessRelease7](json) must
        be(JsSuccess(RoleWithinBusinessRelease7(Set(BeneficialShareholder)), JsPath))
    }

    "successfully validate given values" in {
      val json = Json.obj(
        "roleWithinBusiness" -> Seq(
          "SoleProprietor",
          "NominatedOfficer",
          "DesignatedMember",
          "Director",
          "BeneficialShareholder"
        )
      )

      Json.fromJson[RoleWithinBusinessRelease7](json) must
        be(
          JsSuccess(
            RoleWithinBusinessRelease7(
              Set(SoleProprietor, NominatedOfficer, DesignatedMember, Director, BeneficialShareholder)
            ),
            JsPath
          )
        )
    }
    "successfully validate given all values" in {
      val json = Json.obj(
        "roleWithinBusiness"      -> Seq(
          "BeneficialShareholder",
          "Director",
          "Partner",
          "InternalAccountant",
          "ExternalAccountant",
          "SoleProprietor",
          "NominatedOfficer",
          "DesignatedMember",
          "Other"
        ),
        "roleWithinBusinessOther" -> "some other text"
      )

      Json.fromJson[RoleWithinBusinessRelease7](json) must
        be(
          JsSuccess(
            RoleWithinBusinessRelease7(
              Set(
                BeneficialShareholder,
                Director,
                Partner,
                InternalAccountant,
                ExternalAccountant,
                SoleProprietor,
                NominatedOfficer,
                DesignatedMember,
                Other("some other text")
              )
            ),
            JsPath
          )
        )
    }

    "successfully validate given values with option other details" in {
      val json = Json.obj("roleWithinBusiness" -> Seq("DesignatedMember", "Other"), "roleWithinBusinessOther" -> "test")

      Json.fromJson[RoleWithinBusinessRelease7](json) must
        be(JsSuccess(RoleWithinBusinessRelease7(Set(Other("test"), DesignatedMember)), JsPath))
    }

    "fail when path is missing" in {
      Json.fromJson[RoleWithinBusinessRelease7](Json.obj("roleWithinBusinessOther" -> "other text")) must
        be(JsError((JsPath \ "roleWithinBusiness") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }

    "fail when on invalid data" in {
      Json.fromJson[RoleWithinBusinessRelease7](Json.obj("roleWithinBusiness" -> Set("hello"))) must
        be(JsError((JsPath \ "roleWithinBusiness") -> play.api.libs.json.JsonValidationError("error.invalid")))
    }

    "write valid data in using json write" in {
      val release = RoleWithinBusinessRelease7(Set(SoleProprietor, Other("test657")))

      Json.toJson[RoleWithinBusinessRelease7](release) must be(
        Json.obj("roleWithinBusiness" -> Json.arr("SoleProprietor", "Other"), "roleWithinBusinessOther" -> "test657")
      )
    }
  }
}
