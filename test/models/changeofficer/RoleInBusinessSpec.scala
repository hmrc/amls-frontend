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
      RoleInBusiness.roleFormReads.validate((Seq("06"), None)) mustBe Valid(Set(SoleProprietor))
      RoleInBusiness.roleFormReads.validate((Seq("01"), None)) mustBe Valid(Set(BeneficialOwner))
      RoleInBusiness.roleFormReads.validate((Seq("02"), None)) mustBe Valid(Set(Director))
      RoleInBusiness.roleFormReads.validate((Seq("03"), None)) mustBe Valid(Set(InternalAccountant))
      RoleInBusiness.roleFormReads.validate((Seq("05"), None)) mustBe Valid(Set(Partner))
      RoleInBusiness.roleFormReads.validate((Seq("07"), None)) mustBe Valid(Set(DesignatedMember))
      RoleInBusiness.roleFormReads.validate((Seq("other"), Some("another role"))) mustBe Valid(Set(Other("another role")))
      RoleInBusiness.roleFormReads.validate((Seq(""), None)) mustBe Valid(Set.empty[Role])
    }

    "convert valid form into the model" in {
      val formData = Map(
        "positions[0]" -> Seq("06"),
        "positions[1]" -> Seq("02")
      )

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Valid(RoleInBusiness(Set(SoleProprietor, Director)))
    }

    "convert empty string to empty set" in {
      val formData = Map(
        "positions[0]" -> Seq("")
      )

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Valid(RoleInBusiness(Set.empty[Role]))
    }

    "convert empty form into a set of validation errors" in {
      val formData = Map.empty[String, Seq[String]]
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

    "fail validation when 'other' is specified but no other role is given" in {
      val formData = Map(
        "positions" -> Seq("other")
      )

      val result = RoleInBusiness.formReads.validate(formData)

      result mustBe Invalid(Seq(Path \ "otherPosition" -> Seq(ValidationError("changeofficer.roleinbusiness.validationerror.othermissing"))))
    }

    "successfully write the model to the form" in {
      val formData = Map(
        "positions[]" -> Seq("other"),
        "otherPosition" -> Seq("Some other role")
      )

      val model = RoleInBusiness(Set(Other("Some other role")))

      RoleInBusiness.formWrites.writes(model) mustBe formData
    }

    "convert to Json" in {
      val json = Json.obj(
        "positions" -> Seq(
          "soleprop",
          "partner",
          "other"
        ),
        "otherPosition" -> "Another role"
      )

      val model = RoleInBusiness(Set(SoleProprietor, Partner, Other("Another role")))

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
  }
}
