/*
 * Copyright 2023 HM Revenue & Customs
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
import models.businessmatching.BusinessActivity.{AccountancyServices, BillPaymentServices, EstateAgentBusinessService}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import views.Fixture
import views.html.tradingpremises.what_does_your_business_do


class what_does_your_business_doSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val what_does_your_business_do = app.injector.instanceOf[what_does_your_business_do]
    implicit val requestWithToken = addTokenForView()
  }

  "what_does_your_business_do view" must {

    val businessMatchingActivitiesAll = BusinessMatchingActivities(
      Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

    "have correct title, heading, back link and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.whatdoesyourbusinessdo.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")



      def view = what_does_your_business_do(form2, businessMatchingActivitiesAll, false ,1 )

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.whatdoesyourbusinessdo.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
      doc.select("input[type=checkbox]").size must be(3)

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = what_does_your_business_do(form2, businessMatchingActivitiesAll, true,1)

      errorSummary.html() must include("not a message Key")
    }
  }
}