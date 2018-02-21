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

package views.confirmation

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class BacsConfirmationViewSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    override def view = views.html.confirmation.confirmation_bacs(
      "businessName"
    )
  }

  "The bacs confirmation view" must {

    "show the correct title" in new ViewFixture {
      doc.title must startWith(Messages("confirmation.payment.bacs.title"))
    }

    "show the correct header" in new ViewFixture {
      doc.select(".heading-large").text must include(Messages("confirmation.payment.bacs.header"))
    }

    "show the correct secondary header" in new ViewFixture {
      doc.select(".confirmation p").text must include("businessName")
    }

    "contain the correct content" in new ViewFixture {
      doc.html() must include(Messages("confirmation.payment.info.hmrc_review"))
      doc.html() must include(Messages("confirmation.payment.info.hmrc_review2"))
    }

    "have a footer with the correct information" in new ViewFixture {
      doc.html() must include(Messages("confirmation.payment.info.heading.keep_up_to_date"))
      doc.html() must include(Messages("confirmation.payment.info.keep_up_to_date"))
      doc.html() must include(Messages("confirmation.payment.info.keep_up_to_date.item1"))
      doc.html() must include(Messages("confirmation.payment.info.keep_up_to_date.item2"))
      doc.html() must include(Messages("confirmation.payment.info.keep_up_to_date.item3"))
      doc.getElementsByClass("print-link").first().text() mustBe "Print"
      doc.getElementsByClass("button").first().text() mustBe Messages("confirmation.payment.continue_button.text")
    }

  }

}
