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

package views.bankdetails

import forms.EmptyForm
import models.bankdetails._
import models.status._
import org.jsoup.nodes.Element
import org.scalacheck.Gen
import org.scalatest.MustMatchers
import org.scalatest.prop.PropertyChecks
import play.api.i18n.Messages
import utils.{AmlsSpec, StatusConstants}
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

class summarySpec extends AmlsSpec with MustMatchers with PropertyChecks with HtmlAssertions {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val toHide = 6

    val accountName = "Account Name"

  }

  "summary view" must {
    "have correct title" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))

      def view = views.html.bankdetails.summary(model, 1)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.bankdetails"))
    }

    "have correct headings" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))

      def view = views.html.bankdetails.summary(model, 1)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.bankdetails"))
    }

    "have correct button text" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))

      def view = views.html.bankdetails.summary(model, 1)

      doc.getElementsByClass("button").html must include(Messages("button.summary.acceptandaddbankaccount"))
    }


    "include the provided data for a UKAccount" in new ViewFixture {

      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))

      def view = views.html.bankdetails.summary(model, 1)

      view.body must contain("My Personal Account")
    }

    "include the provided data for a NonUKAccountNumber" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))

      def view = views.html.bankdetails.summary(model, 1)

    }

    "include the provided data for a NonUKIBANNumber" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))

      def view = views.html.bankdetails.summary(model, 1)
    }
  }
}
