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

package views.asp

import forms.EmptyForm
import models.asp._
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


      def view = views.html.asp.summary(EmptyForm, Asp())

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.asp.summary(EmptyForm, Asp())

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.asp"))
    }

    val sectionChecks = Table[String, Element=>Boolean](
      ("title key", "check"),
      ("asp.services.title", checkListContainsItems(_, Set("asp.service.lbl.01",
                                                            "asp.service.lbl.02",
                                                            "asp.service.lbl.03",
                                                            "asp.service.lbl.04",
                                                            "asp.service.lbl.05"))),
      ("asp.other.business.tax.matters.title", checkElementTextIncludes(_, "lbl.yes"))
    )

    "include the provided data" in new ViewFixture {
      def view = {
        val testdata = Asp(
          Some(ServicesOfBusiness(Set(Accountancy, PayrollServices, BookKeeping, Auditing, FinancialOrTaxAdvice))),
          Some(OtherBusinessTaxMattersYes)
        )

        views.html.asp.summary(EmptyForm, testdata)
      }

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be (None)
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }}
    }
  }
}
