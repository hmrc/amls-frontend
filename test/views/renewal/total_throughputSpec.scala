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

package views.renewal

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.TotalThroughput
import org.jsoup.nodes.Document
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture
import views.html.renewal.total_throughput

class total_throughputSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    override def view = total_throughput(EmptyForm, edit = false)
  }

  trait InvalidFormFixture extends ViewFixture {

    val requiredMsg = Messages("renewal.msb.throughput.selection.required")

    val invalidForm = InvalidForm(
      Map.empty[String, Seq[String]],
      Seq(Path \ "throughput" -> Seq(ValidationError(requiredMsg)))
    )

    override def view = total_throughput(invalidForm, edit = false)
  }

  "The MSB total throughput view" must {
    "display the correct header" in new ViewFixture {
      doc.select(".heading-xlarge").text mustBe Messages("renewal.msb.throughput.header")
    }

    "display the correct secondary header" in new ViewFixture {
      doc.select("header .heading-secondary").text must include(Messages("summary.renewal"))
    }

    "display the correct title" in new ViewFixture {
      doc.title must include(s"${Messages("renewal.msb.throughput.header")} - ${Messages("summary.renewal")}")
    }

    "display the informational text" in new ViewFixture {
      doc.body().text must include(Messages("renewal.msb.throughput.info"))
    }

    TotalThroughput.throughputValues foreach { selection =>

      val getElement = (doc: Document) => doc.select(s"""input[type="radio"][name="throughput"][value="${selection.value}"]""")

      s"display the radio button for selection ${selection.value}" in new ViewFixture {
        Option(getElement(doc).first) mustBe defined
      }

      s"display the selection label for selection ${selection.value}" in new ViewFixture {
        val radioLabelElement = getElement(doc).first.parent

        Option(radioLabelElement) mustBe defined
        radioLabelElement.text mustBe Messages(selection.label)
      }
    }

    "display the 'save and continue' button" in new ViewFixture {
      doc.select("""button[type=submit][name=submit]""").text mustBe Messages("button.saveandcontinue")
    }

    "display the error summary" in new InvalidFormFixture {
      val summaryElement = doc.getElementsByClass("amls-error-summary").first
      Option(summaryElement) mustBe defined
      summaryElement.text must include(requiredMsg)
    }

    "display the validation error next to the field" in new InvalidFormFixture {
      val validationMsg = doc.select("#throughput .error-notification").first
      Option(validationMsg) mustBe defined
      validationMsg.text must include(requiredMsg)
    }

    "have a back link" in new ViewFixture {

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
