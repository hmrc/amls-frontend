/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture


class additional_extra_address_UKSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "additional_extra_address view" must {

    "have a back link" in new ViewFixture {
      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))
      def view = views.html.responsiblepeople.address.additional_extra_address_UK(form2, true, 1, None, "firstName lastName")
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = views.html.responsiblepeople.address.additional_extra_address_UK(form2, true, 1, None, "firstName lastName")

      doc.title must startWith (Messages("responsiblepeople.additional_extra_address_country.title", "firstName lastName"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = views.html.responsiblepeople.address.additional_extra_address_UK(form2, true, 1, None, "firstName lastName")

      heading.html must be(Messages("responsiblepeople.additional_extra_address_country.heading", "firstName lastName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" when {
      "UK" in new ViewFixture {

        val form2: InvalidForm = InvalidForm(Map.empty,
          Seq(
            (Path \ "addressLine1") -> Seq(ValidationError("second not a message Key")),
            (Path \ "addressLine2") -> Seq(ValidationError("third not a message Key")),
            (Path \ "addressLine3") -> Seq(ValidationError("fourth not a message Key")),
            (Path \ "addressLine4") -> Seq(ValidationError("fifth not a message Key")),
            (Path \ "postCode") -> Seq(ValidationError("sixth not a message Key"))
          ))

        def view = views.html.responsiblepeople.address.additional_extra_address_UK(form2, true, 1, None, "firstName lastName")

        errorSummary.html() must include("second not a message Key")
        errorSummary.html() must include("third not a message Key")
        errorSummary.html() must include("fourth not a message Key")
        errorSummary.html() must include("fifth not a message Key")
        errorSummary.html() must include("sixth not a message Key")


        doc.getElementById("addressLine1").parent()
          .getElementsByClass("error-notification").first().html() must include("second not a message Key")

        doc.getElementById("addressLine2").parent()
          .getElementsByClass("error-notification").first().html() must include("third not a message Key")

        doc.getElementById("addressLine3").parent()
          .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

        doc.getElementById("addressLine4").parent()
          .getElementsByClass("error-notification").first().html() must include("fifth not a message Key")

        doc.getElementById("postCode").parent()
          .getElementsByClass("error-notification").first().html() must include("sixth not a message Key")

      }

    }
  }
}