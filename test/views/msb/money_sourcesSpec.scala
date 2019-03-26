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

package views.msb

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.moneyservicebusiness.MoneySources
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture


class money_sourcesSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "money_sources view" must {

    "have the back link button" in new ViewFixture {
      val formData: ValidForm[MoneySources] = Form2(MoneySources())
      def view = views.html.msb.money_sources(formData, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {
      val formData: ValidForm[MoneySources] = Form2(MoneySources())
      def view = views.html.msb.money_sources(formData, true)

      doc.title must startWith(Messages("msb.supply_foreign_currencies.title") + " - " + Messages("summary.msb"))
    }

    "have correct headings" in new ViewFixture {
      val formData: ValidForm[MoneySources] = Form2(MoneySources())
      def view = views.html.msb.money_sources(formData, true)

      heading.html must be(Messages("msb.supply_foreign_currencies.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "ask the user who will supply the foreign currency" in new ViewFixture {
      val formData: ValidForm[MoneySources] = Form2(MoneySources())
      def view = views.html.msb.money_sources(formData, true)

      Option(doc.getElementById("bankMoneySource-Yes")).isDefined must be(true)
      Option(doc.getElementById("wholesalerMoneySource-Yes")).isDefined must be(true)
      Option(doc.getElementById("customerMoneySource-Yes")).isDefined must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "WhoWillSupply") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = views.html.msb.money_sources(form2, true)
      errorSummary.html() must include("third not a message Key")

      doc.getElementById("WhoWillSupply")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

    }
  }
}