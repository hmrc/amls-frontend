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

package views.responsiblepeople

import forms.responsiblepeople.SoleProprietorFormProvider
import models.responsiblepeople.SoleProprietorOfAnotherBusiness
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.SoleProprietorView

class SoleProprietorViewSpec extends AmlsViewSpec with Matchers {

  lazy val sole_proprietor = inject[SoleProprietorView]
  lazy val fp              = inject[SoleProprietorFormProvider]

  val name = "Person Name"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "SoleProprietorView" must {

    "have correct title" in new ViewFixture {

      def view = sole_proprietor(fp().fill(SoleProprietorOfAnotherBusiness(true)), true, 1, None, name)

      doc.title must be(
        messages("responsiblepeople.sole.proprietor.another.business.title") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )

      doc.getElementsByAttributeValue("name", "soleProprietorOfAnotherBusiness") must not be empty
    }

    "have correct headings" in new ViewFixture {

      def view = sole_proprietor(fp().fill(SoleProprietorOfAnotherBusiness(true)), true, 1, None, name)

      heading.html    must be(messages("responsiblepeople.sole.proprietor.another.business.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    behave like pageWithErrors(
      sole_proprietor(
        fp().withError("soleProprietorOfAnotherBusiness", "error.required.rp.sole_proprietor"),
        true,
        1,
        None,
        name
      ),
      "soleProprietorOfAnotherBusiness",
      "error.required.rp.sole_proprietor"
    )

    behave like pageWithBackLink(sole_proprietor(fp(), true, 1, None, name))
  }
}
