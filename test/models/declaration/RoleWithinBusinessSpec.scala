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

import models.CharacterSets
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import utils.AmlsSpec

class RoleWithinBusinessSpec extends AmlsSpec with CharacterSets {

  "JSON" must {

    "Read the json and return the RoleWithinBusiness domain object successfully for the BeneficialShareholder" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "01"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(BeneficialShareholder, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the Director" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "02"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(Director, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the ExternalAccountant" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "03"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(ExternalAccountant, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the InternalAccountant" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "04"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(InternalAccountant, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the NominatedOfficer" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "05"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(NominatedOfficer, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the Partner" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "06"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(Partner, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the SoleProprietor" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "07"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(SoleProprietor, JsPath))
    }

    "Read the json and return the given `other` value" in {

      val json = Json.obj(
        "roleWithinBusiness"      -> "08",
        "roleWithinBusinessOther" -> "any other value"
      )

      Json.fromJson[RoleWithinBusiness](json) must
        be(JsSuccess(Other("any other value"), JsPath \ "roleWithinBusinessOther"))
    }

    "Read the json and return error if an invalid value is found" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "10"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(
        JsError(JsPath -> play.api.libs.json.JsonValidationError("error.invalid"))
      )
    }

    "Write the json successfully from the BeneficialShareholder domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = BeneficialShareholder
      val json                                   = Json.obj(
        "roleWithinBusiness" -> "01"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the Director domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = Director
      val json                                   = Json.obj(
        "roleWithinBusiness" -> "02"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the ExternalAccountant domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = ExternalAccountant
      val json                                   = Json.obj(
        "roleWithinBusiness" -> "03"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the InternalAccountant domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = InternalAccountant
      val json                                   = Json.obj(
        "roleWithinBusiness" -> "04"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the NominatedOfficer domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = NominatedOfficer
      val json                                   = Json.obj(
        "roleWithinBusiness" -> "05"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the Partner domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = Partner
      val json                                   = Json.obj(
        "roleWithinBusiness" -> "06"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the SoleProprietor domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = SoleProprietor
      val json                                   = Json.obj(
        "roleWithinBusiness" -> "07"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the Other domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = Other("any other value")
      val json                                   = Json.obj(
        "roleWithinBusiness"      -> "08",
        "roleWithinBusinessOther" -> "any other value"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

  }

}
