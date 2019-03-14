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

package views.businessdetails

import forms.EmptyForm
import models.businessdetails._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.MustMatchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import utils.AmlsSpec
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

class summarySpec extends AmlsSpec
  with MustMatchers
  with HtmlAssertions
  with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.businessdetails.summary(EmptyForm, AboutTheBusiness(), true)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.businessdetails.summary(EmptyForm, AboutTheBusiness(), true)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.businessdetails"))
    }

    "does not show registered for mlr question when approved" in new ViewFixture {
      def view = views.html.businessdetails.summary(EmptyForm, AboutTheBusiness(), false)

      html must not include Messages("businessdetails.registeredformlr.title")
    }

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      ("businessdetails.registeredformlr.title",checkElementTextIncludes(_, "businessdetails.registeredformlr.mlrregno.lbl", "1234")),
      ("businessdetails.activity.start.date.title",checkElementTextIncludes(_, "lbl.start.date", "2 January 2016")),
      ("businessdetails.registeredforvat.title",checkElementTextIncludes(_, "lbl.vat.reg.number", "2345")),
      ("businessdetails.registeredoffice.title",checkElementTextIncludes(_, "line1","line2","line3","line4","AB12CD")),
      ("businessdetails.contactingyou.email.title",checkElementTextIncludes(_, "businessdetails.contactingyou.email.lbl", "test@test.com")),
      ("businessdetails.contactingyou.phone.title",checkElementTextIncludes(_, "businessdetails.contactingyou.phone.lbl", "01234567890")),
      ("businessdetails.correspondenceaddress.postal.address",
        checkElementTextIncludes(_, "your name", "business name","line1","line2","line3","line4","AB12CD"))
    )

    "include the provided data" in new ViewFixture {

      def view = views.html.businessdetails.summary(
        EmptyForm,
        AboutTheBusiness(
          Some(PreviouslyRegisteredYes("1234")),
          Some(ActivityStartDate(new LocalDate(2016, 1, 2))),
          Some(VATRegisteredYes("2345")),
          Some(CorporationTaxRegisteredYes("3456")),
          Some(ContactingYou(Some("01234567890"), Some("test@test.com"))),
          Some(RegisteredOfficeUK("line1","line2",Some("line3"),Some("line4"),"AB12CD")),
          Some(true),
          Some(UKCorrespondenceAddress("your name", "business name","line1","line2",Some("line3"),Some("line4"),"AB12CD")),
          false
        ),true
      )

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")

        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be None
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }
      }
    }
  }
}
