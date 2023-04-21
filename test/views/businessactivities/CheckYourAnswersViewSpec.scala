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

package views.businessactivities

import forms.EmptyForm
import models.businessactivities.TransactionTypes.{DigitalSoftware, DigitalSpreadsheet, Paper}
import models.businessactivities._
import models.businessmatching.{BusinessMatching, BusinessActivities => BMBusinessActivities}
import models.businessmatching.BusinessActivity.MoneyServiceBusiness
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.AmlsSummaryViewSpec
import utils.businessactivities.CheckYourAnswersHelper
import views.Fixture
import views.html.businessactivities.CheckYourAnswersView

import scala.collection.JavaConversions._

class CheckYourAnswersViewSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  lazy val summary = app.injector.instanceOf[CheckYourAnswersView]
  lazy val cyaHelper = app.injector.instanceOf[CheckYourAnswersHelper]
  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView(FakeRequest())
  }

  "summary view" must {
    "have correct title" in new ViewFixture {

      def view = summary(SummaryList())

      doc.title must startWith(messages("title.cya") + " - " + messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {

      def view = summary(SummaryList())

      heading.html must be(messages("title.cya"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    "include the provided data" in new ViewFixture {

    val sectionChecks: Seq[(String, String)] = Seq(
      ("title key", "check"),
      ("businessactivities.confirm-activities.title", "lbl.yes"),
      ("businessactivities.confirm-activities.lbl.details", "OtherActivities"),
      ("businessactivities.business-turnover.title", "businessactivities.business-turnover.lbl.01"),

      (messages("businessactivities.turnover.heading", "money service business"), "businessactivities.turnover.lbl.01"),

      ("businessactivities.businessfranchise.title", "lbl.yes"),
      ("businessactivities.businessfranchise.lbl.franchisename", "FranchiseName"),

      ("businessactivities.employees.line1.cya", "123"),
      ("businessactivities.employees.line2.cya", "456"),
      ("businessactivities.keep.customer.records.title", "lbl.yes"),
      ("businessactivities.do.keep.records",
        Seq("businessactivities.transactiontype.lbl.01", "businessactivities.transactiontype.lbl.02", "businessactivities.transactiontype.lbl.03").mkString),
      ("businessactivities.name.software.pkg.lbl", "SoftwareName"),
      ("businessactivities.identify-suspicious-activity.title", "lbl.yes"),
      ("businessactivities.ncaRegistered.title", "lbl.yes"),
      ("businessactivities.riskassessment.policy.title",  "lbl.yes"),
      ("businessactivities.document.riskassessment.policy.title",
         Seq("businessactivities.RiskAssessmentType.lbl.01", "businessactivities.RiskAssessmentType.lbl.02").mkString),
      ("businessactivities.accountantForAMLSRegulations.title", "lbl.yes"),
      ("businessactivities.whoisyouraccountant.title",
         Seq("AccountantName","tradingName").mkString),
      ("businessactivities.whoisyouraccountant.address.header",
         Seq("line1","line2","line3","line4","AB12CD").mkString),
      (messages("businessactivities.tax.matters.summary.title", "AccountantName"), "lbl.yes")
    )

      val list: SummaryList = cyaHelper.createSummaryList(
        BusinessActivities(
          involvedInOther = Some(InvolvedInOtherYes("OtherActivities")),
          expectedBusinessTurnover = Some(ExpectedBusinessTurnover.First),
          expectedAMLSTurnover = Some(ExpectedAMLSTurnover.First),
          businessFranchise = Some(BusinessFranchiseYes("FranchiseName")),
          transactionRecord = Some(true),
          customersOutsideUK = None, // This is only present in renewal
          ncaRegistered = Some(NCARegistered(true)),
          accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
          identifySuspiciousActivity = Some(IdentifySuspiciousActivity(true)),
          riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(Digital, PaperBased)))),
          howManyEmployees = Some(HowManyEmployees(Some("123"), Some("456"))),
          whoIsYourAccountant = Some(WhoIsYourAccountant(
            Some(WhoIsYourAccountantName("AccountantName", Some("tradingName"))),
            Some(WhoIsYourAccountantIsUk(true)),
            Some(UkAccountantsAddress("line1", "line2", Some("line3"), Some("line4"), "AB12CD")))),
          taxMatters = Some(TaxMatters(true)),
          transactionRecordTypes = Some(TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("SoftwareName"))))
        ),
        BusinessMatching(activities = Some(BMBusinessActivities(Set(MoneyServiceBusiness)))),
        false
      )

      def view = summary(list)

      doc.getElementsByClass("govuk-summary-list__key").toSeq.zip(
        doc.getElementsByClass("govuk-summary-list__value").toSeq
      ).foreach { case (key, value) =>

        val maybeRow = list.rows.find(_.key.content.asHtml.body == key.text()).value

        maybeRow.key.content.asHtml.body must include(key.text())

        val valueText = maybeRow.value.content.asHtml.body match {
          case str if str.startsWith("<") => Jsoup.parse(str).text()
          case str => str
        }

        valueText must include(value.text())
      }
    }
  }
}