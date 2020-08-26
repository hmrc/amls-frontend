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

package views.renewal

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{AccountancyServices, BusinessActivities}
import models.renewal.AMLSTurnover
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.amls_turnover


class amls_turnoverSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val amls_turnover = app.injector.instanceOf[amls_turnover]
    implicit val requestWithToken = addTokenForView()
  }

  "amls_turnover view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Fifth)

      def view = amls_turnover(form2, true, None)

      doc.title must startWith(Messages("renewal.turnover.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings for single service" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Third)

      def view = amls_turnover(form2, true, Some(List("some provider")))

      heading.html must be(Messages("renewal.turnover.title.single.service", "some provider"))
      subHeading.html must include( Messages("summary.renewal"))
    }

    "have correct headings for multiple services" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Third)

      def view = amls_turnover(form2, true, Some(List("some provider", "some other provider")))

      heading.html must be(Messages("renewal.turnover.title"))
      subHeading.html must include( Messages("summary.renewal"))
    }

    "correctly list business activities" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Fifth)

      def view = amls_turnover(form2, true, Some(List("a service provider")))

      html must include("a service provider")
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "turnover") -> Seq(ValidationError("not a message Key"))
        ))

      def view = amls_turnover(form2, true, Some(List("some provider")))

      errorSummary.html() must include("not a message Key")

      doc.getElementById("turnover")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = amls_turnover(EmptyForm, true, None)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
