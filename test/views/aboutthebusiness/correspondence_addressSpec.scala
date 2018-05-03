/*
 * Copyright 2018 HM Revenue & Customs
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

package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.{UKCorrespondenceAddress, CorrespondenceAddress}
import org.scalatest.{MustMatchers}
import  utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class correspondence_addressSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }


  "correspondence_address view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddress] = Form2(UKCorrespondenceAddress(
        "Name",
        "BusinessName",
        "addressLine1",
        "addressLine1",
        None,
        None,
        "AB12CD"
      ))

      def view = views.html.aboutthebusiness.correspondence_address(form2, true)

      doc.title must startWith(Messages("aboutthebusiness.correspondenceaddress.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddress] = Form2(UKCorrespondenceAddress(
        "Name",
        "BusinessName",
        "addressLine1",
        "addressLine1",
        None,
        None,
        "AB12CD"
      ))
      def view = views.html.aboutthebusiness.correspondence_address(form2, true)

      heading.html must be(Messages("aboutthebusiness.correspondenceaddress.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "correspondenceaddress-fieldset") -> Seq(ValidationError("not a message Key")),
          (Path \ "isUK") -> Seq(ValidationError("second not a message Key")),
          (Path \ "postCode-fieldset") -> Seq(ValidationError("third not a message Key")),
          (Path \ "country-fieldset") -> Seq(ValidationError("fourth not a message Key"))
        ))

      def view = views.html.aboutthebusiness.correspondence_address(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fourth not a message Key")

      doc.getElementById("correspondenceaddress-fieldset")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("isUK")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

      doc.getElementById("postCode-fieldset")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

      val test = doc.getElementById("country-fieldset")
        .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

    }
  }
}