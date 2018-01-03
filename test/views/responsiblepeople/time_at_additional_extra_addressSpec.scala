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

package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.responsiblepeople.TimeAtAddress
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class time_at_additional_extra_addressSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "FirstName LastName"
  }

  "time_at_additional_extra_address view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[TimeAtAddress] = Form2(ZeroToFiveMonths)

      def view = views.html.responsiblepeople.time_at_additional_extra_address(form2, false, 0, None, name)

      doc.title must be(Messages("responsiblepeople.timeataddress.address_history.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct heading" in new ViewFixture {

      val form2: ValidForm[TimeAtAddress] = Form2(ZeroToFiveMonths)

      def view = views.html.responsiblepeople.time_at_additional_extra_address(form2, false, 0, None, name)

      heading.html() must be(Messages("responsiblepeople.timeataddress.address_history.heading", name))
    }

    "show errors in correct places when validation fails" in new ViewFixture {

      val messageKey1 = "definitely not a message key"

      val timeAtAddress = "timeAtAddress"

      val form2: InvalidForm = InvalidForm(
        Map("x" -> Seq("y")),
        Seq((Path \ timeAtAddress, Seq(ValidationError(messageKey1))))
      )

      def view = views.html.responsiblepeople.time_at_additional_extra_address(form2, false, 0, None, name)

      errorSummary.html() must include(messageKey1)
    }
  }


}
