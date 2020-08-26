/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessType
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.position_within_business

class position_within_businessSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val position_within_business = app.injector.instanceOf[position_within_business]
    implicit val requestWithToken = addTokenForView()
    val name = "firstName lastName"
  }

  "position_within_business view" must {

    "have back link" in new ViewFixture {
      val form2 = EmptyForm
      def view = position_within_business(form2, true, 1, BusinessType.SoleProprietor, name, true, None)
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {
      val form2 = EmptyForm
      def view = position_within_business(form2, true, 1, BusinessType.SoleProprietor, name, true, None)
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
      doc.title must be(Messages("responsiblepeople.position_within_business.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {
      val form2 = EmptyForm
      def view = position_within_business(form2, true, 1, BusinessType.SoleProprietor, name, true, None)
      heading.html must be(Messages("responsiblepeople.position_within_business.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))
    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "positions") -> Seq(ValidationError("not a message Key"))))

      def view = position_within_business(form2, true, 1, BusinessType.SoleProprietor, name, true, None)
      errorSummary.html() must include("not a message Key")
    }
  }
}
