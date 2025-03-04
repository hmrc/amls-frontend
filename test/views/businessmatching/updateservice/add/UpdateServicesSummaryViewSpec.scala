/*
 * Copyright 2024 HM Revenue & Customs
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

import models.businessmatching.BusinessActivity._
import models.flowmanagement.AddBusinessTypeFlowModel
import utils.UpdateServicesSummaryFixtures
import views.html.businessmatching.updateservice.add.UpdateServicesSummaryView

class UpdateServicesSummaryViewSpec extends UpdateServicesSummaryFixtures {

  lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]

  "UpdateServicesSummaryView" must {
    "have the correct title" in new ViewFixture {
      def view = update_services_summary(AddBusinessTypeFlowModel())
      doc.title must startWith(messages("title.cya") + " - " + messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      def view = update_services_summary(AddBusinessTypeFlowModel())
      heading.html must be(messages("title.cya"))
    }

    "have correct subHeading" in new ViewFixture {
      def view = update_services_summary(AddBusinessTypeFlowModel())
      subHeading.html must include(messages("summary.updateservice"))
    }
  }

  "for which business type you wish to register" must {
    "have a question title" in new ViewFixture {
      val addBusinessTypeFlowModel: AddBusinessTypeFlowModel =
        AddBusinessTypeFlowModel(activity = Some(AccountancyServices))
      def view                                               = update_services_summary(addBusinessTypeFlowModel)

      doc.body().text must include(messages("businessmatching.updateservice.selectactivities.title"))
    }

    "show edit link" in new SimpleFlowModelViewFixture {
      Option(doc.getElementById("selectactivities-edit")).isDefined mustBe true
    }

    "for all business types" must {
      "show AccountancyServices if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel =
          AddBusinessTypeFlowModel(activity = Some(AccountancyServices))

        def view = update_services_summary(addBusinessTypeFlowModel)

        doc.getElementsByClass("govuk-summary-list__value").first.text mustBe messages(
          "businessmatching.registerservices.servicename.lbl.01"
        )
      }

      "show BillPaymentServices if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel =
          AddBusinessTypeFlowModel(activity = Some(BillPaymentServices))

        def view = update_services_summary(addBusinessTypeFlowModel)

        doc.getElementsByClass("govuk-summary-list__value").first.text mustBe (messages(
          "businessmatching.registerservices.servicename.lbl.03"
        ))
      }

      "show EstateAgentBusinessService if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel =
          AddBusinessTypeFlowModel(activity = Some(EstateAgentBusinessService))

        def view = update_services_summary(addBusinessTypeFlowModel)

        doc.getElementsByClass("govuk-summary-list__value").first.text mustBe (messages(
          "businessmatching.registerservices.servicename.lbl.04"
        ))
      }

      "show HighValueDealing if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel =
          AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

        def view = update_services_summary(addBusinessTypeFlowModel)

        doc.getElementsByClass("govuk-summary-list__value").first.text mustBe (messages(
          "businessmatching.registerservices.servicename.lbl.05"
        ))
      }

      "show MoneyServiceBusiness if present" in new MSBAllViewFixture {

        doc.getElementsByClass("govuk-summary-list__value").first.text mustBe (messages(
          "businessmatching.registerservices.servicename.lbl.06"
        ))
      }

      "show TrustAndCompanyServices if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel =
          AddBusinessTypeFlowModel(activity = Some(TrustAndCompanyServices))

        def view = update_services_summary(addBusinessTypeFlowModel)

        doc.getElementsByClass("govuk-summary-list__value").first.text mustBe (messages(
          "businessmatching.registerservices.servicename.lbl.07"
        ))
      }

      "show TelephonePaymentService if present" in new ViewFixture {
        val addBusinessTypeFlowModel: AddBusinessTypeFlowModel =
          AddBusinessTypeFlowModel(activity = Some(TelephonePaymentService))

        def view = update_services_summary(addBusinessTypeFlowModel)

        doc.getElementsByClass("govuk-summary-list__value").first.text mustBe (messages(
          "businessmatching.registerservices.servicename.lbl.08"
        ))
      }
    }
  }

  "if adding msb" must {
    "which services does your business provide" must {
      "have a question title" in new MSBAllViewFixture {
        doc.body().text must include(messages("businessmatching.updateservice.msb.services.title"))
      }

      "show edit link" in new MSBAllViewFixture {
        Option(doc.getElementById("msbservices-edit")).isDefined mustBe true
      }

      "for all msb services" must {
        "show msb service TransmittingMoney" in new MSBAllViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must include(
            messages("businessmatching.services.list.lbl.01")
          )
        }

        "show msb service CurrencyExchange" in new MSBAllViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must include(
            messages("businessmatching.services.list.lbl.02")
          )
        }

        "show msb service ChequeCashingNotScrapMetal" in new MSBAllViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must include(
            messages("businessmatching.services.list.lbl.03")
          )
        }

        "show msb service ChequeCashingScrapMetal" in new MSBAllViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must include(
            messages("businessmatching.services.list.lbl.04")
          )
        }

        "show msb service ForeignExchange" in new MSBAllViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must include(
            messages("businessmatching.services.list.lbl.05")
          )
        }
      }

      "for single msb" must {
        "show msb service TransmittingMoney" in new MSBSingleViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must include(
            messages("businessmatching.services.list.lbl.01")
          )
        }

        "show msb service CurrencyExchange" in new MSBSingleViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must not include (messages(
            "businessmatching.services.list.lbl.02"
          ))
        }

        "show msb service ChequeCashingNotScrapMetal" in new MSBSingleViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must not include (messages(
            "businessmatching.services.list.lbl.03"
          ))
        }

        "show msb service ChequeCashingScrapMetal" in new MSBSingleViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must not include (messages(
            "businessmatching.services.list.lbl.04"
          ))
        }

        "show msb service ForeignExchange" in new MSBSingleViewFixture {
          doc.getElementsByClass("govuk-summary-list__value").get(1).text must not include (messages(
            "businessmatching.services.list.lbl.05"
          ))
        }
      }
    }

    "if adding TransmittingMoney as an MSB subsector, for does your business have a PSR number" must {
      "have a question title" in new MSBAllViewFixture {
        doc.body().text must include(messages("businessmatching.psr.number.title"))
      }

      "show edit link" in new MSBAllViewFixture {
        Option(doc.getElementById("psr-edit")).isDefined mustBe true
      }

      "have Yes answer to PSR number question" in new MSBAllViewFixture {
        doc.getElementsByClass("govuk-summary-list__value").get(2).text mustBe messages("lbl.yes")
      }

      "have a PSR number" in new MSBAllViewFixture {
        doc.getElementsByClass("govuk-summary-list__value").get(3).text mustBe "111111"
      }

      "have answered no to do I have a PSR number" in new MSBNoPSRViewFixture {
        doc.getElementsByClass("govuk-summary-list__value").get(2).text mustBe messages("lbl.no")
      }

      "not have transmitting money and not have a PSR section" in new MSBViewFixture {
        doc.getElementsByClass("govuk-summary-list__value").size() mustBe 2
      }
    }
  }

  "if not msb" in new SimpleFlowModelViewFixture {
    doc.body().text must not include (messages("businessmatching.services.title"))
  }

  "The page have a submit button with correct text" in new MSBAllViewFixture {
    doc.getElementById("updatesummary-submit").text mustBe messages("button.checkyouranswers.acceptandcomplete")
  }
}
