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
import models.businessmatching.updateservice.{ResponsiblePeopleFitAndProper, TradingPremisesActivities}
import models.businessmatching._
import models.flowmanagement.AddBusinessTypeFlowModel
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.AmlsSpec
import views.Fixture
import views.html.businessmatching.updateservice.add._
import utils.UpdateServicesSummaryFixtures

class update_services_summarySpec  extends AmlsSpec with MustMatchers with UpdateServicesSummaryFixtures {
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
      val addBusinessTypeFlowModel:AddBusinessTypeFlowModel  = AddBusinessTypeFlowModel(activity = Some(AccountancyServices))
      def view = update_services_summary(EmptyForm, addBusinessTypeFlowModel)

      doc.body().text must include(Messages("businessmatching.updateservice.selectactivities.title"))
    }

    "show edit link" in new SimpleFlowModelViewFixture {
      Option(doc.getElementById("selectactivities-edit")).isDefined mustBe true
    }

    "for all business types" must {
      "show AccountancyServices if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel = AddBusinessTypeFlowModel(activity = Some(AccountancyServices))

        def view = update_services_summary(EmptyForm, addBusinessTypeFlowModel)

        doc.getElementById("activity-name").text mustBe (Messages("businessmatching.registerservices.servicename.lbl.01"))
      }


      "show BillPaymentServices if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel = AddBusinessTypeFlowModel(activity = Some(BillPaymentServices))

        def view = update_services_summary(EmptyForm, addBusinessTypeFlowModel)

        doc.getElementById("activity-name").text mustBe (Messages("businessmatching.registerservices.servicename.lbl.02"))
      }

      "show EstateAgentBusinessService if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel = AddBusinessTypeFlowModel(activity = Some(EstateAgentBusinessService))

        def view = update_services_summary(EmptyForm, addBusinessTypeFlowModel)

        doc.getElementById("activity-name").text mustBe (Messages("businessmatching.registerservices.servicename.lbl.03"))
      }

      "show HighValueDealing if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

        def view = update_services_summary(EmptyForm, addBusinessTypeFlowModel)

        doc.getElementById("activity-name").text mustBe (Messages("businessmatching.registerservices.servicename.lbl.04"))
      }

      "show MoneyServiceBusiness if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel = AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness))

        def view = update_services_summary(EmptyForm, addBusinessTypeFlowModel)

        doc.getElementById("activity-name").text mustBe (Messages("businessmatching.registerservices.servicename.lbl.05"))
      }

      "show TrustAndCompanyServices if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel = AddBusinessTypeFlowModel(activity = Some(TrustAndCompanyServices))

        def view = update_services_summary(EmptyForm, addBusinessTypeFlowModel)

        doc.getElementById("activity-name").text mustBe (Messages("businessmatching.registerservices.servicename.lbl.06"))
      }

      "show TelephonePaymentService if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel = AddBusinessTypeFlowModel(activity = Some(TelephonePaymentService))

        def view = update_services_summary(EmptyForm, addBusinessTypeFlowModel)

        doc.getElementById("activity-name").text mustBe (Messages("businessmatching.registerservices.servicename.lbl.07"))
      }
    }
  }

  "if adding msb" must {
    "which services does your business provide" must {
      "have a question title" in new MSBAllViewFixture {
        doc.body().text must include(Messages("businessmatching.services.title"))
      }

      "show edit link" in new MSBAllViewFixture {
        Option(doc.getElementById("msbservices-edit")).isDefined mustBe true
      }

      "for all msb services" must {
        "show msb service TransmittingMoney" in new MSBAllViewFixture {
          doc.getElementById("msb-service").text must include(Messages("businessmatching.services.list.lbl.01"))
        }

        "show msb service CurrencyExchange" in new MSBAllViewFixture {
          doc.getElementById("msb-service").text must include(Messages("businessmatching.services.list.lbl.02"))
        }

        "show msb service ChequeCashingNotScrapMetal" in new MSBAllViewFixture {
          doc.getElementById("msb-service").text must include(Messages("businessmatching.services.list.lbl.03"))
        }

        "show msb service ChequeCashingScrapMetal" in new MSBAllViewFixture {
          doc.getElementById("msb-service").text must include(Messages("businessmatching.services.list.lbl.04"))
        }

        "show msb service ForeignExchange" in new MSBAllViewFixture {
          doc.getElementById("msb-service").text must include(Messages("businessmatching.services.list.lbl.05"))
        }
      }

      "for single msb" must {
        "show msb service TransmittingMoney" in new MSBSingleViewFixture {
          doc.getElementById("msb-service").text must include(Messages("businessmatching.services.list.lbl.01"))
        }

        "show msb service CurrencyExchange" in new MSBSingleViewFixture {
          doc.getElementById("msb-service").text must not include(Messages("businessmatching.services.list.lbl.02"))
        }

        "show msb service ChequeCashingNotScrapMetal" in new MSBSingleViewFixture {
          doc.getElementById("msb-service").text must not include(Messages("businessmatching.services.list.lbl.03"))
        }

        "show msb service ChequeCashingScrapMetal" in new MSBSingleViewFixture {
          doc.getElementById("msb-service").text must not include(Messages("businessmatching.services.list.lbl.04"))
        }

        "show msb service ForeignExchange" in new MSBSingleViewFixture {
          doc.getElementById("msb-service").text must not include(Messages("businessmatching.services.list.lbl.05"))
        }
      }
    }

    "if adding TransmittingMoney as an MSB subsector, for does your business have a PSR number" must {
      "have a question title" in new MSBAllViewFixture {
        doc.body().text must include(Messages("businessmatching.psr.number.title"))
      }

      "show edit link" in new MSBAllViewFixture {
        Option(doc.getElementById("psr-edit")).isDefined mustBe true
      }

      "have a PSR number" in new MSBAllViewFixture {
        doc.getElementById("psr").text mustBe Messages("businessmatching.psr.number.lbl") + ": 111111"
      }

      "have answered no to do I have a PSR number " in new MSBNoPSRViewFixture {
        doc.getElementById("psr").text mustBe Messages("lbl.no")
      }

      "not have transmitting money and not have a PSR section" in new MSBViewFixture {
        Option(doc.getElementById("psr")).isDefined mustBe false
      }
    }

    "for what will your business do at these premises" must {
      "have a question title" in new MSBViewFixture {
        doc.body().text must include(Messages("businessmatching.updateservice.whatdoyoudohere.heading"))
      }

      "for the edit link display when business does more than one type of MSB subservice" in new MSBAllViewFixture {
        Option(doc.getElementById("whatdoyoudohere-edit")).isDefined mustBe true
      }

      "for the edit link not display when business does only one type of MSB subservice" in new SingleSubSectorPSRMSBViewFixture {
        Option(doc.getElementById("whatdoyoudohere-edit")).isDefined mustBe false
      }

      "for all tp msb services" must {
        "show msb service TransmittingMoney" in new MSBAllViewFixture {
          doc.getElementById("tp-msb-services").text must include(Messages("businessmatching.updateservice.msb.services.list.lbl.01"))
        }

        "show msb service CurrencyExchange" in new MSBAllViewFixture {
          doc.getElementById("tp-msb-services").text must include(Messages("businessmatching.updateservice.msb.services.list.lbl.02"))
        }

        "show msb service ChequeCashingNotScrapMetal" in new MSBAllViewFixture {
          doc.getElementById("tp-msb-services").text must include(Messages("businessmatching.updateservice.msb.services.list.lbl.03"))
        }

        "show msb service ChequeCashingScrapMetal" in new MSBAllViewFixture {
          doc.getElementById("tp-msb-services").text must include(Messages("businessmatching.updateservice.msb.services.list.lbl.04"))
        }

        "show msb service ForeignExchange" in new MSBAllViewFixture {
          doc.getElementById("tp-msb-services").text must include(Messages("businessmatching.updateservice.msb.services.list.lbl.05"))
        }
      }

      "for a single tp msb service" must {
        "show msb service TransmittingMoney" in new MSBSingleViewFixture {
          doc.getElementById("tp-msb-services").text must include(Messages("businessmatching.updateservice.msb.services.list.lbl.01"))
        }

        "show msb service CurrencyExchange" in new MSBSingleViewFixture {
          doc.getElementById("tp-msb-services").text must not include(Messages("businessmatching.updateservice.msb.services.list.lbl.02"))
        }

        "show msb service ChequeCashingNotScrapMetal" in new MSBSingleViewFixture {
          doc.getElementById("tp-msb-services").text must not include(Messages("businessmatching.updateservice.msb.services.list.lbl.03"))
        }

        "show msb service ChequeCashingScrapMetal" in new MSBSingleViewFixture {
          doc.getElementById("tp-msb-services").text must not include(Messages("businessmatching.updateservice.msb.services.list.lbl.04"))
        }

        "show msb service ForeignExchange" in new MSBSingleViewFixture {
          doc.getElementById("tp-msb-services").text must not include(Messages("businessmatching.updateservice.msb.services.list.lbl.05"))
        }
      }
    }

    "for have any of your responsible people have passed the HMRC fit and proper test" must {
      "have a question title" in new MSBViewFixture {
        doc.body().text must include(Messages("businessmatching.updateservice.fitandproper.heading", Messages("businessmatching.registerservices.servicename.lbl.05")))
        doc.getElementById("fit-and-proper").text mustBe Messages("lbl.yes")
      }

      "show edit link" in new MSBViewFixture {
        Option(doc.getElementById("fitandproper-edit")).isDefined mustBe true
      }

      "which responsible people have passed the HMRC fit and proper test" must {
        "have a question title" in new MSBViewFixture {
          doc.body().text must include(Messages("businessmatching.updateservice.whichfitandproper.heading"))
          doc.getElementById("fit-and-proper-count").text mustBe Messages("businessmatching.updateservice.summary.whichfitandproper.count", 1)
        }

        "show edit link" in new MSBViewFixture {
          Option(doc.getElementById("whichfitandproper-edit")).isDefined mustBe true
        }
      }
    }
  }

  "if not msb" in new SimpleFlowModelViewFixture {
    doc.body().text must not include(Messages("businessmatching.services.title"))
  }

  "if adding TCSP, for have any of your responsible people have passed the HMRC fit and proper test" must {
    "have a question title" in new SimpleTCSPViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.fitandproper.heading"))
      doc.getElementById("fit-and-proper").text mustBe Messages("lbl.yes")
    }

    "show edit link" in new SimpleTCSPViewFixture {
      Option(doc.getElementById("fitandproper-edit")).isDefined mustBe true
    }

    "which responsible people have passed the HMRC fit and proper test" must {
      "have a question title" in new SimpleTCSPViewFixture {
        doc.body().text must include(Messages("businessmatching.updateservice.whichfitandproper.heading"))
        doc.getElementById("fit-and-proper-count").text mustBe Messages("businessmatching.updateservice.summary.whichfitandproper.count.plural", 2)
      }

      "show edit link" in new SimpleTCSPViewFixture {
        Option(doc.getElementById("whichfitandproper-edit")).isDefined mustBe true
      }
    }

    "if no fit and proper then no responsible people section" in new SimpleTCSPNoFitAndProperViewFixture {
      Option(doc.getElementById("fit-and-proper-count")).isDefined mustBe false
      doc.getElementById("fit-and-proper").text mustBe Messages("lbl.no")
    }
  }

  "if not msb or tcsp then no fit and proper section" in new SimpleFlowModelViewFixture {
    Option(doc.getElementById("fit-and-proper")).isDefined mustBe false
    Option(doc.getElementById("fit-and-proper-count")).isDefined mustBe false
  }

  "for will you do this business type at trading premises" must {
    "have a question title" in new MSBViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.tradingpremises.summary", Messages("businessmatching.registerservices.servicename.lbl.05").toLowerCase))
    }

    "show edit link" in new MSBViewFixture {
      Option(doc.getElementById("tradingpremises-edit")).isDefined mustBe true
    }

    "state no registered trading premises" in new MSBViewNoPremisesFixture {
      doc.getElementById("tp-premises-activities").text mustBe Messages("lbl.no")
    }

    "not show premises details sections" in new MSBViewNoPremisesFixture {
      Option(doc.getElementById("tp-premises-activities-count")).isDefined mustBe false
      Option(doc.getElementById("tp-msb-services")).isDefined mustBe false
    }
  }

  "if doing business type at trading premises, for which premises will you do this business type from" must {
    "have a question title" in new MSBAllViewFixture {
      doc.body().text must include(Messages("businessmatching.updateservice.whichtradingpremises.summary", Messages("businessmatching.registerservices.servicename.lbl.05").toLowerCase))
    }

    "show edit link" in new MSBAllViewFixture {
      Option(doc.getElementById("whichtradingpremises-edit")).isDefined mustBe true
    }

    "show the message and count" in new MSBAllViewFixture {
      doc.getElementById("tp-premises-activities").text mustBe Messages("lbl.yes")
      doc.getElementById("tp-premises-activities-count").text mustBe Messages("businessmatching.updateservice.summary.whichtradingpremises.count", 2)
    }
  }

  "The page have a submit button with correct text" in new MSBAllViewFixture {
    doc.getElementById("updatesummary-submit").text mustBe Messages("button.checkyouranswers.acceptandcomplete")
  }
}