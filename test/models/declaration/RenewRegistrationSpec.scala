/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class RenewRegistrationSpec extends PlaySpec with MockitoSugar {
  "Form Validation" must {
    "successfully validate given an enum value" in {
      val data = Map("renewRegistration" -> Seq("false"))

      RenewRegistration.formRule.validate(data) must
        be(Valid(RenewRegistrationNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map("renewRegistration" -> Seq("true"))

      RenewRegistration.formRule.validate(data) must
        be(Valid(RenewRegistrationYes))
    }

    "fail to validate mandatory field" in {
      RenewRegistration.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "renewRegistration") -> Seq(ValidationError("error.required.declaration.renew.registration"))
        )))
    }

    "write correct data from enum value" in {
      RenewRegistration.formWrites.writes(RenewRegistrationNo) must
        be(Map("renewRegistration" -> Seq("false")))
    }

    "write correct data from `Yes` value" in {
      RenewRegistration.formWrites.writes(RenewRegistrationYes) must
        be(Map("renewRegistration" -> Seq("true")))
    }
  }

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[RenewRegistration](Json.obj("renewRegistration" -> false)) must
        be(JsSuccess(RenewRegistrationNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {
      val json = Json.obj("renewRegistration" -> true)

      Json.fromJson[RenewRegistration](json) must
        be(JsSuccess(RenewRegistrationYes))
    }

    "successfully validate given an `No` value" in {
      val json = Json.obj("renewRegistration" -> false)

      Json.fromJson[RenewRegistration](json) must
        be(JsSuccess(RenewRegistrationNo))
    }

    "write the correct value" in {
      Json.toJson(RenewRegistrationNo.asInstanceOf[RenewRegistration]) must
        be(Json.obj("renewRegistration" -> false))

      Json.toJson(RenewRegistrationYes.asInstanceOf[RenewRegistration]) must
        be(Json.obj("renewRegistration" -> true))
    }
  }
}
