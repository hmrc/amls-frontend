/*
 * Copyright 2017 HM Revenue & Customs
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

package models.changeofficer

import jto.validation.{Invalid, Valid, ValidationError, Path}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class RoleInBusinessSpec  extends PlaySpec with MustMatchers {

  "RoleInBusiness" must {

    "validate a role" in {
      RoleInBusiness.roleReads.validate("soleprop") mustBe Valid(SoleProprietor)
      RoleInBusiness.roleReads.validate("bensharehold") mustBe Valid(BeneficialShareholder)
      RoleInBusiness.roleReads.validate("director") mustBe Valid(Director)
      RoleInBusiness.roleReads.validate("extAccountant") mustBe Valid(ExternalAccountant)
      RoleInBusiness.roleReads.validate("intAccountant") mustBe Valid(InternalAccountant)
      RoleInBusiness.roleReads.validate("partner") mustBe Valid(Partner)
      RoleInBusiness.roleReads.validate("desigmemb") mustBe Valid(DesignatedMember)
    }

    "convert valid form into the model" in {
      val formData = Map("positions" -> Seq("soleprop"))

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Valid(RoleInBusiness(Set(SoleProprietor)))
    }

    "convert to Json" in {
      val json = Json.obj(
        "positions" -> Seq(
          "soleprop",
          "partner"
        )
      )

      val model = RoleInBusiness(Set(SoleProprietor, Partner))

      Json.toJson(model) mustBe json
    }
  }
}
