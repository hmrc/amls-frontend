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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.CETransactionsInLast12Months
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture

class ce_transactions_in_last_12_monthsSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "ceTransactions view" must {

    val ce = CETransactionsInLast12Months("123")

    "have correct title" in new ViewFixture {

      val form2: ValidForm[CETransactionsInLast12Months] = Form2(ce)

      def view = views.html.renewal.ce_transactions_in_last_12_months(form2, true)

      doc.title must startWith(Messages("renewal.msb.ce.transactions.expected.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CETransactionsInLast12Months] = Form2(ce)

      def view = views.html.renewal.ce_transactions_in_last_12_months(form2, true)

      heading.text() must be(Messages("renewal.msb.ce.transactions.expected.title"))
      subHeading.text() must include(Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "ceTransaction") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.ce_transactions_in_last_12_months(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("ceTransaction")
        .parent.getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = views.html.renewal.ce_transactions_in_last_12_months(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
