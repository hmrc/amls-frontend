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

package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.businessactivities.{UkAccountantsAddress, AccountantsAddress, WhoIsYourAccountant}
import org.scalatest.{MustMatchers}
import utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class who_is_your_accountantSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "who_is_your_accountant view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[WhoIsYourAccountant] = Form2(WhoIsYourAccountant("accountantName",Some("tradingName"),UkAccountantsAddress("line1","line2",None,None,"AB12CD")))

      def view = views.html.businessactivities.who_is_your_accountant(form2, true)

      doc.title must startWith(Messages("businessactivities.whoisyouraccountant.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[WhoIsYourAccountant] = Form2(WhoIsYourAccountant("accountantName",Some("tradingName"),UkAccountantsAddress("line1","line2",None,None,"AB12CD")))

      def view = views.html.businessactivities.who_is_your_accountant(form2, true)

      heading.html must be(Messages("businessactivities.whoisyouraccountant.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "name") -> Seq(ValidationError("not a message Key")),
          (Path \ "tradingName") -> Seq(ValidationError("second not a message Key")),
          (Path \ "isUK") -> Seq(ValidationError("third not a message Key")),
          (Path \ "addressLine1") -> Seq(ValidationError("fourth not a message Key")),
          (Path \ "addressLine2") -> Seq(ValidationError("fifth not a message Key")),
          (Path \ "addressLine3") -> Seq(ValidationError("sixth not a message Key")),
          (Path \ "addressLine4") -> Seq(ValidationError("seventh not a message Key")),
          (Path \ "postCode") -> Seq(ValidationError("eighth not a message Key")),
          (Path \ "country") -> Seq(ValidationError("ninth not a message Key"))
        ))

      def view = views.html.businessactivities.who_is_your_accountant(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fourth not a message Key")
      errorSummary.html() must include("fifth not a message Key")
      errorSummary.html() must include("sixth not a message Key")
      errorSummary.html() must include("seventh not a message Key")
      errorSummary.html() must include("eighth not a message Key")
      errorSummary.html() must include("ninth not a message Key")

      doc.getElementById("name").parent
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("tradingName").parent
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

      doc.getElementById("isUK")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

      doc.getElementById("addressLine1").parent
        .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

      doc.getElementById("addressLine2").parent
        .getElementsByClass("error-notification").first().html() must include("fifth not a message Key")

      doc.getElementById("addressLine3").parent
        .getElementsByClass("error-notification").first().html() must include("sixth not a message Key")

      doc.getElementById("addressLine4").parent
        .getElementsByClass("error-notification").first().html() must include("seventh not a message Key")

      doc.getElementById("postCode").parent
        .getElementsByClass("error-notification").first().html() must include("eighth not a message Key")

      doc.getElementById("country").parent
        .getElementsByClass("error-notification").first().html() must include("ninth not a message Key")

    }
  }
}