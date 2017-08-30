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

package views.declaration

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessType
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class add_personSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "add_person view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AddPerson] = Form2(AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))))

      def view = views.html.declaration.add_person("string1", "string2", Some(BusinessType.LPrLLP), form2)

      doc.title mustBe s"string1 - ${Messages("title.amls")} - ${Messages("title.gov")}"
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[AddPerson] = Form2(AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))))

      def view = views.html.declaration.add_person("string1", "string2", Some(BusinessType.LPrLLP), form2)

      heading.html must be(Messages("declaration.addperson.title"))
      subHeading.html must include(Messages("string2"))
    }

    "pre-populate the fields correctly" in new ViewFixture {
      val role = RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder, models.declaration.release7.ExternalAccountant))
      val person = AddPerson("Forename", Some("Middlename"), "Surname", role)
      val f = Form2[AddPerson](AddPerson.formWrites.writes(person) ++ RoleWithinBusinessRelease7.formWrites.writes(role))

      def view = views.html.declaration.add_person("string 1", "string 2", Some(BusinessType.LPrLLP), f)

      doc.getElementById("firstName").`val` mustBe "Forename"
      doc.getElementById("middleName").`val` mustBe "Middlename"
      doc.getElementById("lastName").`val` mustBe "Surname"
      doc.select("#positions input[checked]").get(0).`val` mustBe "01"
      doc.select("#positions input[checked]").get(1).`val` mustBe "08"
    }

    "pre-populate the 'other' field correctly" in new ViewFixture {
      val f = Form2(AddPerson("Forename", None, "Surname", RoleWithinBusinessRelease7(Set(models.declaration.release7.Other("Other details")))))

      def view = views.html.declaration.add_person("string 1", "string 2", Some(BusinessType.LPrLLP), f)

      doc.getElementById("otherPosition").`val` mustBe "Other details"
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "firstName") -> Seq(ValidationError("not a message Key")),
          (Path \ "middleName") -> Seq(ValidationError("second not a message Key")),
          (Path \ "positions") -> Seq(ValidationError("third not a message Key")),
          (Path \ "otherPosition") -> Seq(ValidationError("fourth not a message Key"))
        ))

      def view = views.html.declaration.add_person("string1", "string2", Some(BusinessType.LPrLLP), form2)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fourth not a message Key")

      doc.getElementById("firstName")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("middleName")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

      doc.getElementById("positions")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

      doc.getElementById("otherPosition")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

    }
  }
}
