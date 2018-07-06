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

import forms.EmptyForm
import models.businessmatching.HighValueDealing
import models.flowmanagement.AddBusinessTypeFlowModel
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture
import views.html.businessmatching.updateservice.add._


class update_services_summarySpec  extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "The update_services_summary view" must {

    "have the correct title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
      heading.html must be(Messages("title.cya"))
    }

    "have correct subHeading" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
      subHeading.html must include(Messages("summary.updateservice"))
    }
  }

  "for which business type you wish to register" must {
    "have a question title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel(Some(HighValueDealing)))
      doc.body().text must include(Messages("businessmatching.updateservice.selectactivities.title"))
    }

    "display the answer" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel(Some(HighValueDealing)))
      doc.body().text must include(Messages("businessmatching.registerservices.servicename.lbl.04"))
    }

    "show edit link" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel(Some(HighValueDealing)))
    }
  }

  "for have any of your responsible people have passed the HMRC fit and proper test" must {
    "have a question title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "display the answer" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "show edit link" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }
  }

  "if any have passed test, which responsible people have passed the HMRC fit and proper test" must {
    "have a question title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "display the answer" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "show edit link" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }
  }

  "for will you do this business type at trading premises" must {
    "have a question title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "display the answer" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "show edit link" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }
  }

  "for what will your business do at these premises" must {
    "have a question title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "display the answer" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "show edit link" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }
  }

  "if adding msb, which services does your business provide" must {
    "have a question title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "display the answer" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "show edit link" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }
  }

  "if adding msb, for what will your business do at these premises" must {
    "have a question title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "display the answer" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "for the edit link not display when business does only one type of MSB subservice" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "for the edit link display when business does more than one type of MSB subservice" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }
  }

  "if adding TransmittingMoney as an MSB subsector, for does your business have a PSR number" must {
    "have a question title" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "display the answer" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }

    "show edit link" in new ViewFixture {
      def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
    }
  }
}