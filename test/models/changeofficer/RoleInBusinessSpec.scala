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
      RoleInBusiness.roleFormReads.validate(Seq("soleprop")) mustBe Valid(Set(SoleProprietor))
      RoleInBusiness.roleFormReads.validate(Seq("bensharehold")) mustBe Valid(Set(BeneficialShareholder))
      RoleInBusiness.roleFormReads.validate(Seq("director")) mustBe Valid(Set(Director))
      RoleInBusiness.roleFormReads.validate(Seq("extAccountant")) mustBe Valid(Set(ExternalAccountant))
      RoleInBusiness.roleFormReads.validate(Seq("intAccountant")) mustBe Valid(Set(InternalAccountant))
      RoleInBusiness.roleFormReads.validate(Seq("partner")) mustBe Valid(Set(Partner))
      RoleInBusiness.roleFormReads.validate(Seq("desigmemb")) mustBe Valid(Set(DesignatedMember))
    }

    "convert valid form into the model" in {
      val formData = Map("positions" -> Seq("soleprop"))

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Valid(RoleInBusiness(Set(SoleProprietor)))
    }

    "convert invalid form into a set of validation errors" in {
      val formData = Map(
        "positions" -> Seq("some value")
      )

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Invalid(Seq(Path \ "positions" -> Seq(ValidationError("changeofficer.roleinbusiness.validationerror"))))
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

    "convert from Json" in {

      val jsonString =
        """
          |{"positions": [
          | "soleprop", "partner"]
          |}
        """.stripMargin

      val model = RoleInBusiness(Set(SoleProprietor, Partner))

      Json.parse(jsonString).as[RoleInBusiness] mustBe model
    }

    "convert from invalid json" in {
      val jsonString =
        """
          | {
          |   "positions": [
          |     "some value"
          |   ]
          | }
        """.stripMargin

      Json.parse(jsonString).validate[RoleInBusiness] mustBe
        JsError(Seq(JsPath \ "positions" -> Seq(play.api.data.validation.ValidationError("changeofficer.roleinbusiness.validationerror"))))
    }
  }
}
