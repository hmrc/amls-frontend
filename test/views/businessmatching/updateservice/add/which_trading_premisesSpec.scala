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

package views.businessmatching.updateservice.add

import forms.{EmptyForm, InvalidForm}
import generators.tradingpremises.TradingPremisesGenerator
import jto.validation.{Path, ValidationError}
import models.businessmatching.{BusinessActivities, HighValueDealing}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import views.html.businessmatching.updateservice.add._

class which_trading_premisesSpec extends GenericTestHelper with MustMatchers with TradingPremisesGenerator {

  val tp = tradingPremisesGen.sample.get

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val activityName = BusinessActivities.getValue(HighValueDealing)

    def view = which_trading_premises(EmptyForm, false, Seq((tp, 0)), activityName)
  }

  "The which_trading_premises view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.whichtradingpremises.title") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.whichtradingpremises.heading",
        Messages(s"businessmatching.registerservices.servicename.lbl."+ activityName + ".phrased")))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }

    "not show the return link" in new ViewFixture {
      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "tradingPremises") -> Seq(ValidationError("not a message Key"))))

      override def view = which_trading_premises(form2, false, Seq((tp, 0)), "01")

      errorSummary.html() must include("not a message Key")

      doc.getElementById("tradingPremises")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }

}
