/*
 * Copyright 2020 HM Revenue & Customs
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

package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import models.tradingpremises.Address
import play.api.i18n.Messages
import views.Fixture


class activit_start_dateSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "activit_start_date view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.startDate.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = {
        val address = Address("line 1", "Line 2", None, None, "postcode")
        views.html.tradingpremises.activity_start_date(form2, 1, false, address)
      }

      val expectedAddressInHtml = "<p> line 1<br> Line 2<br> postcode<br> </p>"

      doc.html must include(expectedAddressInHtml)
      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.startDate.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))
      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      doc.getElementsContainingOwnText(Messages("lbl.day")).hasText must be(true)
      doc.getElementsContainingOwnText(Messages("lbl.month")).hasText must be(true)
      doc.getElementsContainingOwnText(Messages("lbl.year")).hasText must be(true)

      doc.getElementsContainingOwnText(Messages("lbl.date.example")).hasText must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = {
        val address = Address("", "", None, None, "")
        views.html.tradingpremises.activity_start_date(form2, 1, true, address)
      }

      errorSummary.html() must include("not a message Key")
    }
  }
}