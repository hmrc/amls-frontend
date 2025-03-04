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

package views.declaration

import forms.declaration.AddPersonFormProvider
import models.businessmatching.BusinessType
import models.declaration.AddPerson
import models.declaration.release7.{ExternalAccountant, RoleWithinBusinessRelease7}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.AddPersonView

class AddPersonViewSpec extends AmlsViewSpec with Matchers {

  lazy val personView = inject[AddPersonView]
  lazy val fp         = inject[AddPersonFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "AddPersonView" must {
    "have correct title" in new ViewFixture {

      val formWithData = fp().fill(
        AddPerson(
          "FirstName",
          None,
          "LastName",
          RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))
        )
      )

      def view = personView("string1", "string2", Some(BusinessType.LPrLLP), formWithData)

      doc.title mustBe s"string1 - ${messages("title.amls")} - ${messages("title.gov")}"
    }

    "have correct headings" in new ViewFixture {

      val formWithData = fp().fill(
        AddPerson(
          "FirstName",
          None,
          "LastName",
          RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))
        )
      )

      def view = personView("string1", "string2", Some(BusinessType.LPrLLP), formWithData)

      heading.html    must be(messages("declaration.addperson.title"))
      subHeading.html must include(messages("string2"))
    }

    "have correct h2s" in new ViewFixture {

      val formWithData = fp().fill(
        AddPerson(
          "FirstName",
          None,
          "LastName",
          RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))
        )
      )

      def view = personView("string1", "string2", Some(BusinessType.LPrLLP), formWithData)

      doc.getElementsByClass("govuk-heading-m").text()           must include("Name")
      doc.getElementsByClass("govuk-fieldset__legend--m").text() must include("Role in the business")
    }

    "pre-populate the fields correctly" in new ViewFixture {

      val role = RoleWithinBusinessRelease7(
        Set(
          models.declaration.release7.ExternalAccountant
        )
      )

      val person = AddPerson("Forename", Some("Middlename"), "Surname", role)

      val f = fp().fill(person)

      def view = personView("string 1", "string 2", Some(BusinessType.UnincorporatedBody), f)

      doc.getElementById("firstName").`val` mustBe "Forename"
      doc.getElementById("middleName").`val` mustBe "Middlename"
      doc.getElementById("lastName").`val` mustBe "Surname"

      doc.select("input[checked]").get(0).`val` mustBe ExternalAccountant.toString
    }

    "pre-populate the 'other' field correctly" in new ViewFixture {
      val f = fp().fill(
        AddPerson(
          "Forename",
          None,
          "Surname",
          RoleWithinBusinessRelease7(Set(models.declaration.release7.Other("Other details")))
        )
      )

      def view = personView("string 1", "string 2", Some(BusinessType.LPrLLP), f)

      doc.getElementById("otherPosition").`val` mustBe "Other details"
    }

    Seq(
      ("firstName", "error.invalid.firstname.validation"),
      ("middleName", "error.invalid.middlename.validation"),
      ("lastName", "error.invalid.lastname.validation"),
      ("positions", "error.invalid.position.validation"),
      ("otherPosition", "err.text.role.in.business.text.validation")
    ).foreach { case (field, error) =>
      behave like pageWithErrors(
        personView("string 1", "string 2", Some(BusinessType.LPrLLP), fp().withError(field, error)),
        field,
        error
      )
    }

    behave like pageWithBackLink(personView("string 1", "string 2", Some(BusinessType.LPrLLP), fp()))
  }
}
