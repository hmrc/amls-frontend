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

package views.supervision

import forms.EmptyForm
import models.supervision._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

class summarySpec extends GenericTestHelper with MustMatchers with TableDrivenPropertyChecks with HtmlAssertions {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {
    "have correct title" in new ViewFixture {
      def view = views.html.supervision.summary(EmptyForm, Supervision())

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.supervision"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.supervision.summary(EmptyForm, Supervision())

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.supervision"))
    }

    "include the provided data" in new ViewFixture {

      def view = {
        val testdata = Supervision(
          Some(AnotherBodyYes("Company A", new LocalDate(1993, 8, 25), new LocalDate(1999, 8, 25), "Ending reason")),
          Some(ProfessionalBodyMemberYes),
          Some(BusinessTypes(Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("anotherProfessionalBody")))),
          Some(ProfessionalBodyYes("details")),
          hasAccepted = true
        )

        views.html.supervision.summary(EmptyForm, testdata)
      }

      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        ("supervision.another_body.title",checkElementTextIncludes(_, "lbl.yes", "Company A", "25 August 1993", "25 August 1999", "Ending reason")),
        ("supervision.memberofprofessionalbody.title",checkElementTextIncludes(_, "lbl.yes")),
        ("supervision.whichprofessionalbody.title",checkElementTextIncludes(_,
          "supervision.memberofprofessionalbody.lbl.01",
          "supervision.memberofprofessionalbody.lbl.02",
          "supervision.memberofprofessionalbody.lbl.14",
          "anotherProfessionalBody"
        )),
        ("supervision.penalisedbyprofessional.title",checkElementTextIncludes(_, "details"))
      )

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be None
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }}
    }
  }
}
