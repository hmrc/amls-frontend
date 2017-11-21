/*
 * Copyright 2017 HM Revenue & Customs
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

package views.businessactivities

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import utils.BooleanFormReadWrite._

class customer_transaction_recordsSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "customer_transaction_records view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[Boolean] = Form2(false)(formWrites("isRecorded"))

      def view = views.html.businessactivities.customer_transaction_records(form2, true)

      doc.title must startWith(Messages("businessactivities.keep.customer.records.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[Boolean] = Form2(false)(formWrites("isRecorded"))

      def view = views.html.businessactivities.customer_transaction_records(form2, true)

      heading.html must be(Messages("businessactivities.keep.customer.records.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isRecorded") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessactivities.customer_transaction_records(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("isRecorded")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}