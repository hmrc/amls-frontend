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

package views.withdrawal

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class withdrawal_reasonSpec extends GenericTestHelper with MustMatchers  {

  trait TestFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "withdrawal_reasons view" must {
    "have correct title" in new TestFixture {

      def view = views.html.withdrawal.withdrawal_reason(EmptyForm)

      doc.title must be(Messages("withdrawal.reason.title") +
        " - " + Messages("title.yapp") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new TestFixture {

      def view = views.html.withdrawal.withdrawal_reason(EmptyForm)

      heading.html must be(Messages("withdrawal.reason.heading"))
      subHeading.html must include(Messages("summary.status"))

    }

    "have the correct fields" in new TestFixture {

      override def view = views.html.withdrawal.withdrawal_reason(EmptyForm)

      doc.getElementsByAttributeValue("for", "withdrawalReason-01") must not be empty
      doc.getElementsByAttributeValue("for", "withdrawalReason-02") must not be empty
      doc.getElementsByAttributeValue("for", "withdrawalReason-03") must not be empty
      doc.getElementsByAttributeValue("for", "withdrawalReason-04") must not be empty
      doc.getElementsByAttributeValue("name", "specifyOtherReason") must not be empty
    }

    "show errors in the correct locations" in new TestFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "withdrawalReason") -> Seq(ValidationError("not another message key")),
          (Path \ "specifyOtherReason") -> Seq(ValidationError("not yet another message key"))
        ))

      def view = views.html.withdrawal.withdrawal_reason(form2)

      errorSummary.html() must include("not another message key")
      errorSummary.html() must include("not yet another message key")

      doc.getElementById("withdrawalReason")
        .getElementsByClass("error-notification").first().html() must include("not another message key")

      doc.getElementById("specifyOtherReason").parent()
        .getElementsByClass("error-notification").first().html() must include("not yet another message key")

    }
  }
}