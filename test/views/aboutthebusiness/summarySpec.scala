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

package views.aboutthebusiness

import models.aboutthebusiness._
import models.businessmatching.BusinessType
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._


class summarySpec extends GenericTestHelper
  with MustMatchers
  with HtmlAssertions
  with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.aboutthebusiness.summary(AboutTheBusiness(), true, BusinessType.LimitedCompany)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.aboutthebusiness.summary(AboutTheBusiness(), true, BusinessType.LimitedCompany)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.aboutbusiness"))
    }

    "does not show registered for mlr question when approved" in new ViewFixture {
      def view = views.html.aboutthebusiness.summary(AboutTheBusiness(), false, BusinessType.LimitedCompany)

      html must not include Messages("aboutthebusiness.registeredformlr.title")
    }

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      ("aboutthebusiness.registeredformlr.title",checkElementTextIncludes(_, "aboutthebusiness.registeredformlr.mlrregno.lbl", "1234")),
      ("aboutthebusiness.activity.start.date.title",checkElementTextIncludes(_, "lbl.start.date", "2 January 2016")),
      ("aboutthebusiness.registeredforvat.title",checkElementTextIncludes(_, "lbl.vat.reg.number", "2345")),
      ("aboutthebusiness.registeredforcorporationtax.title",checkElementTextIncludes(_, "aboutthebusiness.registeredforcorporationtax.taxReference", "3456")),
      ("aboutthebusiness.registeredoffice.title",checkElementTextIncludes(_, "line1","line2","line3","line4","AB12CD")),
      ("aboutthebusiness.contactingyou.email.title",checkElementTextIncludes(_, "aboutthebusiness.contactingyou.email.lbl", "test@test.com")),
      ("aboutthebusiness.contactingyou.phone.title",checkElementTextIncludes(_, "aboutthebusiness.contactingyou.phone.lbl", "01234567890")),
      ("aboutthebusiness.correspondenceaddress.postal.address",
        checkElementTextIncludes(_, "your name", "business name","line1","line2","line3","line4","AB12CD"))
    )

    "include the provided data" in new ViewFixture {

      def view = views.html.aboutthebusiness.summary(
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
        ),true, BusinessType.LimitedCompany
      )

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")

        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be (None)
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }
      }
    }
  }
}
