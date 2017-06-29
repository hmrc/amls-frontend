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
      RoleInBusiness.roleFormReads.validate((Seq("soleprop"), None)) mustBe Valid(Set(SoleProprietor))
      RoleInBusiness.roleFormReads.validate((Seq("bensharehold"), None)) mustBe Valid(Set(BeneficialShareholder))
      RoleInBusiness.roleFormReads.validate((Seq("director"), None)) mustBe Valid(Set(Director))
      RoleInBusiness.roleFormReads.validate((Seq("extAccountant"), None)) mustBe Valid(Set(ExternalAccountant))
      RoleInBusiness.roleFormReads.validate((Seq("intAccountant"), None)) mustBe Valid(Set(InternalAccountant))
      RoleInBusiness.roleFormReads.validate((Seq("partner"), None)) mustBe Valid(Set(Partner))
      RoleInBusiness.roleFormReads.validate((Seq("desigmemb"), None)) mustBe Valid(Set(DesignatedMember))
      RoleInBusiness.roleFormReads.validate((Seq("other"), Some("another role"))) mustBe Valid(Set(Other("another role")))
    }

    "convert valid form into the model" in {
      val formData = Map(
        "positions[0]" -> Seq("soleprop"),
        "positions[1]" -> Seq("director")
      )

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Valid(RoleInBusiness(Set(SoleProprietor, Director)))
    }

    "convert empty form into a set of validation errors" in {
      val formData = Map.empty[String, Seq[String]]
      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Invalid(Seq(Path \ "positions" -> Seq(ValidationError("changeofficer.roleinbusiness.validationerror"))))
    }

    "convert invalid form into a set of validation errors" in {
      val formData = Map(
        "positions" -> Seq("some value")
      )

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Invalid(Seq(Path \ "positions" -> Seq(ValidationError("changeofficer.roleinbusiness.validationerror"))))
    }

    "convert 'other' option into a valid model" in {
      val formData = Map(
        "positions" -> Seq("other"),
        "otherPosition" -> Seq("Some other role")
      )

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Valid(RoleInBusiness(Set(Other("Some other role"))))
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
          |{
          | "positions": [
          |   "soleprop", "partner", "other"
          | ],
          | "otherPosition": "another position"
          |}
        """.stripMargin

      val model = RoleInBusiness(Set(SoleProprietor, Partner, Other("another position")))

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
