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

package views.supervision

import forms.EmptyForm
import models.supervision._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.MustMatchers
import utils.AmlsSpec
import play.api.i18n.Messages
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

class summarySpec extends AmlsSpec with MustMatchers with TableDrivenPropertyChecks with HtmlAssertions {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val start = Some(SupervisionStart(new LocalDate(1990, 2, 24)))  //scalastyle:off magic.number
    val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24)))//scalastyle:off magic.number
    val reason = Some(SupervisionEndReasons("Ending reason"))
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

    "include the provided data if there is another body provided" in new ViewFixture {

      def view = {
        val testdata = Supervision(
          Some(AnotherBodyYes("Company A", start, end, reason)),
          Some(ProfessionalBodyMemberYes),
          Some(ProfessionalBodies(Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("anotherProfessionalBody")))),
          Some(ProfessionalBodyYes("details")),
          hasAccepted = true
        )

        views.html.supervision.summary(EmptyForm, testdata)
      }

      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        ("supervision.another_body.title",checkElementTextIncludes(_, "Company A")),
        ("supervision.supervision_start.title",checkElementTextIncludes(_, "24 February 1990")),
        ("supervision.supervision_end.title",checkElementTextIncludes(_, "24 February 1998")),
        ("supervision.supervision_end_reasons.title",checkElementTextIncludes(_, "Ending reason")),
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

    "include the provided data if there is no another body provided" in new ViewFixture {

      def view = {
        val testdata = Supervision(
          Some(AnotherBodyNo),
          Some(ProfessionalBodyMemberYes),
          Some(ProfessionalBodies(Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("anotherProfessionalBody")))),
          Some(ProfessionalBodyYes("details")),
          hasAccepted = true
        )

        views.html.supervision.summary(EmptyForm, testdata)
      }

      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        ("supervision.another_body.title",checkElementTextIncludes(_, "lbl.no")),
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
