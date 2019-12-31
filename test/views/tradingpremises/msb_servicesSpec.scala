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

package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbServices, TransmittingMoney}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture


class msb_servicesSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()

    val bmModel = BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))))
  }

  "msb_services view" must {
    "have correct title, heading, back link and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm



      val pageTitle = Messages("tradingpremises.msb.services.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.msb_services(form2, 1, false, false, bmModel)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.msb.services.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
      doc.select("input[type=checkbox]").size mustBe 1
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "msbServices[0]") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.msb_services(form2, 1, true, false, bmModel)

      errorSummary.html() must include("not a message Key")
    }
  }
}