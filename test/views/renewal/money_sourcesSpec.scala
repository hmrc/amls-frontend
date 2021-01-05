/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.MoneySources
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.money_sources


class money_sourcesSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val money_sources = app.injector.instanceOf[money_sources]
    implicit val requestWithToken = addTokenForView()
  }

  "money sources view" must {
    "have correct title" in new ViewFixture {

      val formData: ValidForm[MoneySources] = Form2(MoneySources(None, None, None))

      def view = money_sources(formData, true)

      doc.title must startWith(Messages("renewal.msb.money_sources.header") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val formData: ValidForm[MoneySources] = Form2(MoneySources(None, None, None))

      def view = money_sources(formData, true)

      heading.html must be(Messages("renewal.msb.money_sources.header"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "include the necessary checkboxes" in new ViewFixture {

      val formData: ValidForm[MoneySources] = Form2(MoneySources(None, None, None))

      def view = money_sources(formData, true)

      Option(doc.getElementById("bankMoneySource-Yes")).isDefined must be(true)
      Option(doc.getElementById("wholesalerMoneySource-Yes")).isDefined must be(true)
      Option(doc.getElementById("customerMoneySource-Yes")).isDefined must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "bankNames") -> Seq(ValidationError("third not a message Key")),
          (Path \ "wholesalerNames") -> Seq(ValidationError("fifth not a message Key"))
        ))

      def view = money_sources(form2, true)

      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fifth not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = money_sources(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}