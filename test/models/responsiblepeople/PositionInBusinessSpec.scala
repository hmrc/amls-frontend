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

package models.responsiblepeople

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class PositionInBusinessSpec extends PlaySpec with MockitoSugar {

  "PositionInBusiness" must {

    "JSON validation" must {

      "read the correct value" when {

        "given a BeneficialOwner value" in {
          Json.fromJson[PositionWithinBusiness](JsString("01")) must
            be(JsSuccess(BeneficialOwner))
        }

        "given a Director value" in {
          Json.fromJson[PositionWithinBusiness](JsString("02")) must
            be(JsSuccess(Director))
        }

        "given a InternalAccountant value" in {
          Json.fromJson[PositionWithinBusiness](JsString("03")) must
            be(JsSuccess(InternalAccountant))
        }

        "given a NominatedOfficer value" in {
          Json.fromJson[PositionWithinBusiness](JsString("04")) must
            be(JsSuccess(NominatedOfficer))
        }

        "given a Partner value" in {
          Json.fromJson[PositionWithinBusiness](JsString("05")) must
            be(JsSuccess(Partner))
        }

        "given a SoleProprietor value" in {
          Json.fromJson[PositionWithinBusiness](JsString("06")) must
            be(JsSuccess(SoleProprietor))
        }

        "given a DesignatedMember value" in {
          Json.fromJson[PositionWithinBusiness](JsString("07")) must
            be(JsSuccess(DesignatedMember))
        }

        "given an OtherSelection value" in {
          Json.fromJson[PositionWithinBusiness](Json.obj("other" -> "some other role")) mustBe JsSuccess(
            Other("some other role")
          )
        }
      }

      "fail to validate" when {
        "given an empty value" in {
          Json.fromJson[PositionWithinBusiness](JsString("")) must
            be(JsError((JsPath \ "positions") -> play.api.libs.json.JsonValidationError("error.invalid")))
        }
      }

      "write the correct value" when {

        "given a BeneficialOwner" in {
          Json.toJson(BeneficialOwner.asInstanceOf[PositionWithinBusiness]) must be(JsString("01"))
        }

        "given a Director" in {
          Json.toJson(Director.asInstanceOf[PositionWithinBusiness]) must be(JsString("02"))
        }

        "given a InternalAccountant" in {
          Json.toJson(InternalAccountant.asInstanceOf[PositionWithinBusiness]) must be(JsString("03"))
        }

        "given a NominatedOfficer" in {
          Json.toJson(NominatedOfficer.asInstanceOf[PositionWithinBusiness]) must be(JsString("04"))
        }

        "given a Partner" in {
          Json.toJson(Partner.asInstanceOf[PositionWithinBusiness]) must be(JsString("05"))
        }

        "given a SoleProprietor" in {
          Json.toJson(SoleProprietor.asInstanceOf[PositionWithinBusiness]) must be(JsString("06"))
        }

        "given a DesignatedMember" in {
          Json.toJson(DesignatedMember.asInstanceOf[PositionWithinBusiness]) must be(JsString("07"))
        }

        "given an Other" in {
          Json.toJson(Other("some new role").asInstanceOf[PositionWithinBusiness]) mustBe Json.obj(
            "other" -> "some new role"
          )
        }
      }
    }
  }
}
