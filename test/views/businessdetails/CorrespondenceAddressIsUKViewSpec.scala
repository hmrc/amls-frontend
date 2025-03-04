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

import forms.businessdetails.CorrespondenceAddressIsUKFormProvider
import models.autocomplete.NameValuePair
import models.businessdetails.CorrespondenceAddressIsUk
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.CorrespondenceAddressIsUKView

class CorrespondenceAddressIsUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val correspondence_address_is_uk: CorrespondenceAddressIsUKView = inject[CorrespondenceAddressIsUKView]
  lazy val formProvider: CorrespondenceAddressIsUKFormProvider         = inject[CorrespondenceAddressIsUKFormProvider]

  implicit val request: Request[_] = FakeRequest()
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

      val formWithData = formProvider().fill(CorrespondenceAddressIsUk(true))

      def view = correspondence_address_is_uk(formWithData, true)

      doc.title must startWith(
        messages("businessdetails.correspondenceaddress.isuk.title") + " - " + messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val formWithData = formProvider().fill(CorrespondenceAddressIsUk(true))

      def view = correspondence_address_is_uk(formWithData, true)

      heading.html    must be(messages("businessdetails.correspondenceaddress.isuk.title"))
      subHeading.html must include(messages("summary.businessdetails"))

    }

    behave like pageWithErrors(
      correspondence_address_is_uk(formProvider().bind(Map("isUk" -> "")), true),
      "isUk",
      "businessdetails.correspondenceaddress.isuk.error"
    )

    behave like pageWithBackLink(correspondence_address_is_uk(formProvider(), true))
  }
}
