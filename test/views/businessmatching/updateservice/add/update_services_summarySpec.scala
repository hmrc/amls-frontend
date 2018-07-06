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
import models.businessmatching.updateservice.ResponsiblePeopleFitAndProper
import models.businessmatching._
import models.flowmanagement.AddBusinessTypeFlowModel
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture
import views.html.businessmatching.updateservice.add._


class update_services_summarySpec  extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
    def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
  }

  trait SimpleFlowModelViewFixture extends ViewFixture {
    override def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel(
      activity = Some(HighValueDealing),
      areNewActivitiesAtTradingPremises = Some(true)
    ))
  }

  trait SimpleTCSPViewFixture extends ViewFixture {
    override def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel(
      activity = Some(TrustAndCompanyServices),
      fitAndProper = Some(true),
      responsiblePeople = Some(ResponsiblePeopleFitAndProper(Set(1, 2)))
    ))
  }

  trait MSBViewFixture extends ViewFixture {
    override def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel(
      activity = Some(MoneyServiceBusiness),
      fitAndProper = Some(true),
      responsiblePeople = Some(ResponsiblePeopleFitAndProper(Set(1, 2))),
      subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))),
      businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("111111")),
      areNewActivitiesAtTradingPremises = Some(true),
      tradingPremisesMsbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange)))
    ))
  }

  trait SingleSubSectorMSBViewFixture extends ViewFixture {
    override def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel(
      activity = Some(MoneyServiceBusiness),
      responsiblePeople = Some(ResponsiblePeopleFitAndProper(Set(1, 2))),
      subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
      businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("111111")),
      areNewActivitiesAtTradingPremises = Some(true)
    ))
  }

  "The update_services_summary view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("title.cya"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }
  }

  "for which business type you wish to register" must {
    "have a question title" in new SimpleFlowModelViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.selectactivities.title"))
    }

    "show edit link" in new SimpleFlowModelViewFixture {
      Option(doc.getElementById("selectactivities-edit")).isDefined mustBe true
    }
  }

  "if adding TCSP, for have any of your responsible people have passed the HMRC fit and proper test" must {
    "have a question title" in new SimpleTCSPViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.fitandproper.heading", Messages("businessmatching.registerservices.servicename.lbl.06")))
    }

    "show edit link" in new SimpleTCSPViewFixture {
      Option(doc.getElementById("fitandproper-edit")).isDefined mustBe true
    }
  }

  "if adding TCSP and any have passed test, which responsible people have passed the HMRC fit and proper test" must {
    "have a question title" in new SimpleTCSPViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.whichfitandproper.heading", Messages("businessmatching.registerservices.servicename.lbl.06")))
    }

    "show edit link" in new SimpleTCSPViewFixture {
      Option(doc.getElementById("whichfitandproper-edit")).isDefined mustBe true
    }
  }

  "if adding MSB, for have any of your responsible people have passed the HMRC fit and proper test" must {
    "have a question title" in new MSBViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.fitandproper.heading", Messages("businessmatching.registerservices.servicename.lbl.05")))
    }

    "show edit link" in new MSBViewFixture {
      Option(doc.getElementById("fitandproper-edit")).isDefined mustBe true
    }
  }

  "if adding MSB and any have passed test, which responsible people have passed the HMRC fit and proper test" must {
    "have a question title" in new MSBViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.whichfitandproper.heading", Messages("businessmatching.registerservices.servicename.lbl.05")))
    }

    "show edit link" in new MSBViewFixture {
      Option(doc.getElementById("whichfitandproper-edit")).isDefined mustBe true
    }
  }

  "for will you do this business type at trading premises" must {
    "have a question title" in new SimpleFlowModelViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.tradingpremises.summary", Messages("businessmatching.registerservices.servicename.lbl.04.phrased")))
    }

    "show edit link" in new SimpleFlowModelViewFixture {
      Option(doc.getElementById("tradingpremises-edit")).isDefined mustBe true
    }
  }

  "if doing business type at trading premises, for which premises will you do this business type from" must {
    "have a question title" in new SimpleFlowModelViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.whichtradingpremises.summary", Messages("businessmatching.registerservices.servicename.lbl.04.phrased")))
    }

    "show edit link" in new SimpleFlowModelViewFixture {
      Option(doc.getElementById("whichtradingpremises-edit")).isDefined mustBe true
    }
  }

  "if adding msb, which services does your business provide" must {
    "have a question title" in new MSBViewFixture {
      doc.body().text must include(Messages("businessmatching.services.title"))
    }

    "show edit link" in new MSBViewFixture {
      Option(doc.getElementById("msbservices-edit")).isDefined mustBe true
    }
  }

  "if adding msb, for what will your business do at these premises" must {
    "have a question title" in new MSBViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.whatdoyoudohere.heading", Messages("businessmatching.registerservices.servicename.lbl.05")))
    }

    "for the edit link display when business does more than one type of MSB subservice" in new MSBViewFixture {
      Option(doc.getElementById("whatdoyoudohere-edit")).isDefined mustBe true
    }

    "for the edit link not display when business does only one type of MSB subservice" in new SingleSubSectorMSBViewFixture {
      Option(doc.getElementById("whatdoyoudohere-edit")).isDefined mustBe false
    }
  }

  "if adding TransmittingMoney as an MSB subsector, for does your business have a PSR number" must {
    "have a question title" in new MSBViewFixture {
      doc.body().text must include(Messages("businessmatching.psr.number.title"))
    }

    "show edit link" in new MSBViewFixture {
      Option(doc.getElementById("psr-edit")).isDefined mustBe true
    }
  }
}