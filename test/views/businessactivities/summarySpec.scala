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

package views.businessactivities

import forms.EmptyForm
import models.businessactivities._
import models.businessmatching.{BusinessMatching, MoneyServiceBusiness, BusinessActivities => BMBusinessActivities}
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import views.Fixture

import scala.collection.JavaConversions._

class summarySpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView(FakeRequest())
  }

  "summary view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.businessactivities.summary(EmptyForm, BusinessActivities(), None, true)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.businessactivities.summary(EmptyForm, BusinessActivities(), None, true)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "include the provided data" in new ViewFixture {

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      ("businessactivities.confirm-activities.title",checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.confirm-activities.lbl.details",checkElementTextIncludes(_, "OtherActivities")),
      ("businessactivities.business-turnover.title",checkElementTextIncludes(_, "businessactivities.business-turnover.lbl.01")),

      (Messages("businessactivities.turnover.heading", "money service business"),checkElementTextIncludes(_, "businessactivities.turnover.lbl.01")),

      ("businessactivities.businessfranchise.title",checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.businessfranchise.lbl.franchisename",checkElementTextIncludes(_, "FranchiseName")),

      ("businessactivities.employees.line1.cya",checkElementTextIncludes(_, "123")),
      ("businessactivities.employees.line2.cya",checkElementTextIncludes(_, "456")),
      ("businessactivities.keep.customer.records.title", checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.do.keep.records",
        checkElementTextIncludes(_, "businessactivities.transactiontype.lbl.01", "businessactivities.transactiontype.lbl.02", "businessactivities.transactiontype.lbl.03")),
      ("businessactivities.name.software.pkg.lbl",
        checkElementTextIncludes(_, "SoftwareName")),
      ("businessactivities.identify-suspicious-activity.title",checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.ncaRegistered.title",checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.riskassessment.policy.title", checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.document.riskassessment.policy.title",
        checkElementTextIncludes(_, "businessactivities.RiskAssessmentType.lbl.01", "businessactivities.RiskAssessmentType.lbl.02")),
      ("businessactivities.accountantForAMLSRegulations.title",checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.whoisyouraccountant.title",
        checkElementTextIncludes(_, "AccountantName","tradingName")),
      ("businessactivities.whoisyouraccountant.address.header",
        checkElementTextIncludes(_, "line1","line2","line3","line4","AB12CD")),
      ("businessactivities.tax.matters.summary.title",checkElementTextIncludes(_, "AccountantName", "lbl.yes"))
    )

      def view = views.html.businessactivities.summary(
        f = EmptyForm,
        model = BusinessActivities(
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
        bmBusinessActivities = BusinessMatching(activities = Some(BMBusinessActivities(Set(MoneyServiceBusiness)))),
        hideReceiveAdvice = false
      )

      forAll(sectionChecks) { (key, check) => {
        val questions = doc.select("span.bold")

        val question = questions.toList.find(e => e.text() == Messages(key, "AccountantName"))

        question must not be None
        val section = question.get.parents().select("div").first()
        check(section) must be(true)
      }
      }
    }
  }
}