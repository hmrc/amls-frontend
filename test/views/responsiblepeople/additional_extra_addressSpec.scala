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

package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{PersonAddressNonUK, PersonAddressUK, ResponsiblePersonAddress}
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.Country
import play.api.i18n.Messages
import views.Fixture


class additional_extra_addressSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "additional_extra_address view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, None, "firstName lastName")

      doc.title must startWith (Messages("responsiblepeople.additional_extra_address.title", "firstName lastName"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, None, "firstName lastName")

      heading.html must be(Messages("responsiblepeople.additional_extra_address.heading", "firstName lastName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" when {
      "UK" in new ViewFixture {

        val form2: InvalidForm = InvalidForm(Map.empty,
          Seq(
            (Path \ "isUK") -> Seq(ValidationError("not a message Key")),
            (Path \ "addressLine1") -> Seq(ValidationError("second not a message Key")),
            (Path \ "addressLine2") -> Seq(ValidationError("third not a message Key")),
            (Path \ "addressLine3") -> Seq(ValidationError("fourth not a message Key")),
            (Path \ "addressLine4") -> Seq(ValidationError("fifth not a message Key")),
            (Path \ "postCode") -> Seq(ValidationError("sixth not a message Key"))
          ))

        def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, None, "firstName lastName")

        errorSummary.html() must include("not a message Key")
        errorSummary.html() must include("second not a message Key")
        errorSummary.html() must include("third not a message Key")
        errorSummary.html() must include("fourth not a message Key")
        errorSummary.html() must include("fifth not a message Key")
        errorSummary.html() must include("sixth not a message Key")

        doc.getElementById("isUK")
          .getElementsByClass("error-notification").first().html() must include("not a message Key")

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

      "non UK" in new ViewFixture {

        val form2: InvalidForm = InvalidForm(Map.empty,
          Seq(
            (Path \ "isUK") -> Seq(ValidationError("not a message Key")),
            (Path \ "addressLineNonUK1") -> Seq(ValidationError("second not a message Key")),
            (Path \ "addressLineNonUK2") -> Seq(ValidationError("third not a message Key")),
            (Path \ "addressLineNonUK3") -> Seq(ValidationError("fourth not a message Key")),
            (Path \ "addressLineNonUK4") -> Seq(ValidationError("fifth not a message Key")),
            (Path \ "country") -> Seq(ValidationError("sixth not a message Key"))
          ))

        def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, None, "firstName lastName")

        errorSummary.html() must include("not a message Key")
        errorSummary.html() must include("second not a message Key")
        errorSummary.html() must include("third not a message Key")
        errorSummary.html() must include("fourth not a message Key")
        errorSummary.html() must include("fifth not a message Key")
        errorSummary.html() must include("sixth not a message Key")

        doc.getElementById("isUK")
          .getElementsByClass("error-notification").first().html() must include("not a message Key")

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