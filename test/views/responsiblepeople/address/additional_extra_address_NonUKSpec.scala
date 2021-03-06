/*
 * Copyright 2021 HM Revenue & Customs
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

package views.responsiblepeople.address

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.autocomplete.NameValuePair
import models.responsiblepeople.{PersonAddressUK, ResponsiblePersonAddress}
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import play.api.i18n.Messages
import views.Fixture
import views.html.responsiblepeople.address.additional_extra_address_NonUK


class additional_extra_address_NonUKSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val additional_extra_address_NonUK = app.injector.instanceOf[additional_extra_address_NonUK]
    implicit val requestWithToken = addTokenForView()

    val countries = Some(Seq(
      NameValuePair("Country 1", "country:1")
    ))
  }

  "additional_extra_address view" must {

    "have a back link" in new ViewFixture {
      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))
      def view = additional_extra_address_NonUK(form2, true, 1, None, "firstName lastName", countries)
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = additional_extra_address_NonUK(form2, true, 1, None, "firstName lastName", countries)

      doc.title must startWith (Messages("responsiblepeople.additional_extra_address_country.title", "firstName lastName"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = additional_extra_address_NonUK(form2, true, 1, None, "firstName lastName", countries)

      heading.html must be(Messages("responsiblepeople.additional_extra_address_country.heading", "firstName lastName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" when {
      "non UK" in new ViewFixture {

        val form2: InvalidForm = InvalidForm(Map.empty,
          Seq(
            (Path \ "addressLineNonUK1") -> Seq(ValidationError("second not a message Key")),
            (Path \ "addressLineNonUK2") -> Seq(ValidationError("third not a message Key")),
            (Path \ "addressLineNonUK3") -> Seq(ValidationError("fourth not a message Key")),
            (Path \ "addressLineNonUK4") -> Seq(ValidationError("fifth not a message Key")),
            (Path \ "country") -> Seq(ValidationError("sixth not a message Key"))
          ))

        def view = additional_extra_address_NonUK(form2, true, 1, None, "firstName lastName", countries)
        errorSummary.html() must include("second not a message Key")
        errorSummary.html() must include("third not a message Key")
        errorSummary.html() must include("fourth not a message Key")
        errorSummary.html() must include("fifth not a message Key")
        errorSummary.html() must include("sixth not a message Key")

        doc.getElementById("addressLineNonUK1").parent()
          .getElementsByClass("error-notification").first().html() must include("second not a message Key")

        doc.getElementById("addressLineNonUK2").parent()
          .getElementsByClass("error-notification").first().html() must include("third not a message Key")

        doc.getElementById("addressLineNonUK3").parent()
          .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

        doc.getElementById("addressLineNonUK4").parent()
          .getElementsByClass("error-notification").first().html() must include("fifth not a message Key")

        doc.getElementById("country").parent()
          .getElementsByClass("error-notification").first().html() must include("sixth not a message Key")

      }
    }
  }
}