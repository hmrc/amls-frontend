/*
 * Copyright 2023 HM Revenue & Customs
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

package views.asp

import forms.EmptyForm
import models.asp._
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import views.Fixture
import views.html.asp.summary

import scala.collection.JavaConversions._

class summarySpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    lazy val summary = app.injector.instanceOf[summary]
    implicit val requestWithToken = addTokenForView(FakeRequest())
  }

  "summary view" must {
    "have correct title" in new ViewFixture {


      def view = summary(EmptyForm, Asp())

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {
      def view = summary(EmptyForm, Asp())

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.asp"))
    }

    "include the provided data" in new ViewFixture {

      val sectionChecks = Table[String, Element=>Boolean](
        ("title key", "check"),
        ("asp.services.title", checkListContainsItems(_, Set("asp.service.lbl.01",
          "asp.service.lbl.02",
          "asp.service.lbl.03",
          "asp.service.lbl.04",
          "asp.service.lbl.05"))),
        ("asp.other.business.tax.matters.title", checkElementTextIncludes(_, "lbl.yes"))
      )

      def view = {
        val testdata = Asp(
          Some(ServicesOfBusiness(Set(Accountancy, PayrollServices, BookKeeping, Auditing, FinancialOrTaxAdvice))),
          Some(OtherBusinessTaxMattersYes)
        )

        summary(EmptyForm, testdata)
      }

      forAll(sectionChecks) { (key, check) => {
        val elements = doc.select("span.bold")
        val maybeElement = elements.toList.find(e => e.text() == Messages(key))

        maybeElement must not be (None)
        val section = maybeElement.get.parents().select("div").first()
        check(section) must be(true)
      }}
    }
  }
}
