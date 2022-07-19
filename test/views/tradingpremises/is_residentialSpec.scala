/*
 * Copyright 2022 HM Revenue & Customs
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
import jto.validation.{Path, ValidationError}
import models.Country
import models.businesscustomer.Address
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.is_residential


class is_residentialSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val is_residential = app.injector.instanceOf[is_residential]
    implicit val requestWithToken = addTokenForView()

    val address=Address("56 Southview Road", "Newcastle Upon Tyne", Some("Tyne and Wear"), Some ("Whitehill"), Some("NE3 6JAX"), Country(
      "United Kingdom", "UK"
    ))
  }

  "is_residentialSpec view" must {

      "have correct title, heading, back link and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.isResidential.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = is_residential(form2,Some(address), 1, false)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.isResidential.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

        doc.getElementsByAttributeValue("class", "link-back") must not be empty
        doc.select("input[type=radio]").size() must be(2)

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = is_residential(form2,Some(address), 1, true)

      errorSummary.html() must include("not a message Key")
    }

    "the have a residential address" in new ViewFixture {
      def view = is_residential(EmptyForm, Some(address),1, false)

      doc.html() must include("Whitehill")
    }

  }
}