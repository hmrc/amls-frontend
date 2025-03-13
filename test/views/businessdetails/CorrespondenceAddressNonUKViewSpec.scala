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

package views.businessdetails

import forms.businessdetails.CorrespondenceAddressNonUKFormProvider
import models.Country
import models.businessdetails.CorrespondenceAddressNonUk
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.CorrespondenceAddressNonUKView

class CorrespondenceAddressNonUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val correspondence_address_non_uk = app.injector.instanceOf[CorrespondenceAddressNonUKView]
  lazy val formProvider                  = app.injector.instanceOf[CorrespondenceAddressNonUKFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val countries = Seq(
    SelectItem(text = "Country 1", value = Some("country:1"))
  )

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "correspondence_address view" must {
    "have correct title" in new ViewFixture {

      val formWithData = formProvider().fill(
        CorrespondenceAddressNonUk(
          "Name",
          "BusinessName",
          "addressLine1",
          Some("addressLine2"),
          None,
          None,
          Country("AB12CD", "XX")
        )
      )

      def view = correspondence_address_non_uk(formWithData, true, countries)

      doc.title must startWith(
        messages("businessdetails.correspondenceaddress.title") + " - " + messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val formWithData = formProvider().fill(
        CorrespondenceAddressNonUk(
          "Name",
          "BusinessName",
          "addressLine1",
          Some("addressLine2"),
          None,
          None,
          Country("Antarctica", "XX")
        )
      )

      def view = correspondence_address_non_uk(formWithData, true, countries)

      heading.html    must be(messages("businessdetails.correspondenceaddress.title"))
      subHeading.html must include(messages("summary.businessdetails"))
    }

    behave like pageWithErrors(
      correspondence_address_non_uk(
        formProvider().withError("addressLine1", "error.required.address.line1"),
        true,
        countries
      ),
      "addressLine1",
      "error.required.address.line1"
    )

    behave like pageWithBackLink(correspondence_address_non_uk(formProvider(), true, countries))
  }
}
