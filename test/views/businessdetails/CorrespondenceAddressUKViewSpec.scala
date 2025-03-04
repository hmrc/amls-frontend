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

import forms.businessdetails.CorrespondenceAddressUKFormProvider
import models.autocomplete.NameValuePair
import models.businessdetails.CorrespondenceAddressUk
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.CorrespondenceAddressUKView

class CorrespondenceAddressUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val correspondence_address_uk: CorrespondenceAddressUKView = inject[CorrespondenceAddressUKView]
  lazy val formProvider: CorrespondenceAddressUKFormProvider      = inject[CorrespondenceAddressUKFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
    val countries                                                  = Some(
      Seq(
        NameValuePair("Country 1", "country:1")
      )
    )
  }

  "correspondence_address view" must {
    "have correct title" in new ViewFixture {
      val formWithData: Form[CorrespondenceAddressUk] = formProvider().fill(
        CorrespondenceAddressUk("Name", "BusinessName", "addressLine1", Some("addressLine2"), None, None, "AB12CD")
      )

      def view = correspondence_address_uk(formWithData, true)

      doc.title must startWith(
        messages("businessdetails.correspondenceaddress.title") + " - " + messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val formWithData = formProvider().fill(
        CorrespondenceAddressUk(
          "Name",
          "BusinessName",
          "addressLine1",
          Some("addressLine2"),
          None,
          None,
          "AB12CD"
        )
      )
      def view         = correspondence_address_uk(formWithData, true)

      heading.html    must be(messages("businessdetails.correspondenceaddress.title"))
      subHeading.html must include(messages("summary.businessdetails"))
    }

    val formWithError = formProvider().bind(
      Map(
        "yourName"     -> "John Doe",
        "businessName" -> "Big Corp Inc",
        "addressLine1" -> "123 Test Flat",
        "addressLine2" -> "Test Street",
        "addressLine3" -> "Test Town",
        "addressLine4" -> "Test City",
        "postCode"     -> ""
      )
    )

    behave like pageWithErrors(
      correspondence_address_uk(formWithError, true),
      "postCode",
      "error.required.postcode"
    )

    behave like pageWithBackLink(correspondence_address_uk(formProvider(), true))
  }
}
