/*
 * Copyright 2024 HM Revenue & Customs
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

package views.renewal

import forms.renewal.TotalThroughputFormProvider
import models.moneyservicebusiness.ExpectedThroughput
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.TotalThroughputView

class TotalThroughputViewSpec extends AmlsViewSpec with Matchers {

  lazy val total_throughput                                      = inject[TotalThroughputView]
  lazy val fp                                                    = inject[TotalThroughputFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {
    override def view = total_throughput(fp(), edit = false)
  }

  "The MSB total throughput view" must {
    "display the correct header" in new ViewFixture {
      heading.text mustBe messages("renewal.msb.throughput.header")
    }

    "display the correct secondary header" in new ViewFixture {
      subHeading.text must include(messages("summary.renewal"))
    }

    "display the correct title" in new ViewFixture {
      doc.title must include(s"${messages("renewal.msb.throughput.header")} - ${messages("summary.renewal")}")
    }

    "display the informational text" in new ViewFixture {
      doc.html must include(messages("renewal.msb.throughput.info.prefix"))
      doc.html must include(messages("renewal.msb.throughput.info.bold"))
      doc.html must include(messages("renewal.msb.throughput.info.suffix"))
    }

    ExpectedThroughput.all.zipWithIndex foreach { case (selection, index) =>
      val getElement =
        (doc: Document) => doc.select(s"""input[type="radio"][name="throughput"][value="${selection.toString}"]""")

      s"display the radio button for selection ${selection.toString}" in new ViewFixture {
        Option(getElement(doc).first) mustBe defined
      }

      s"display the selection label for selection ${selection.toString}" in new ViewFixture {
        val radioLabelElement = getElement(doc).first.parent

        Option(radioLabelElement) mustBe defined
        radioLabelElement.text mustBe messages(s"renewal.msb.throughput.selection.${index + 1}")
      }
    }

    "display the 'save and continue' button" in new ViewFixture {
      doc.getElementById("button").text mustBe messages("button.saveandcontinue")
    }

    behave like pageWithErrors(
      total_throughput(fp().withError("throughput", "renewal.msb.throughput.selection.required"), false),
      "throughput",
      "renewal.msb.throughput.selection.required"
    )

    behave like pageWithBackLink(total_throughput(fp(), false))
  }
}
