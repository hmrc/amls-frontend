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
import models.Country
import models.moneyservicebusiness.MostTransactions
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.renewal.most_transactions

class most_transactionsSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks{
    lazy val most_transactions = app.injector.instanceOf[most_transactions]
    implicit val requestWithToken = addTokenForView()
  }

  "most_transactions view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[MostTransactions] = Form2(MostTransactions(Seq.empty[Country]))

      def view = most_transactions(form2, true, mockAutoComplete.getCountries)

      doc.title must startWith(Messages("renewal.msb.most.transactions.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[MostTransactions] = Form2(MostTransactions(Seq.empty[Country]))

      def view = most_transactions(form2, true, mockAutoComplete.getCountries)

      heading.html must be(Messages("renewal.msb.most.transactions.title"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "mostTransactionsCountries") -> Seq(ValidationError("not a message Key"))
        ))

      def view = most_transactions(form2, true, mockAutoComplete.getCountries)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("mostTransactionsCountries")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = most_transactions(EmptyForm, true, mockAutoComplete.getCountries)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}