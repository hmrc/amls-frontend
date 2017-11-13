/*
 * Copyright 2017 HM Revenue & Customs
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
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._


class summarySpec extends GenericTestHelper
  with MustMatchers
  with HtmlAssertions
  with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
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

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      ("businessactivities.confirm-activities.title",checkElementTextIncludes(_, "OtherActivities")),
      ("businessactivities.business-turnover.title",checkElementTextIncludes(_, "businessactivities.turnover.lbl.01")),
      ("businessactivities.turnover.title",checkElementTextIncludes(_, "businessactivities.business-turnover.lbl.01")),
      ("businessactivities.businessfranchise.title",checkElementTextIncludes(_, "FranchiseName")),
      ("businessactivities.employees.line1.cya",checkElementTextIncludes(_, "123")),
      ("businessactivities.employees.line2.cya",checkElementTextIncludes(_, "456")),
      ("businessactivities.keep.customer.records.title",
        checkElementTextIncludes(_, "businessactivities.transactiontype.lbl.01", "businessactivities.transactiontype.lbl.02", "businessactivities.transactiontype.lbl.03", "SoftwareName")),
      ("businessactivities.identify-suspicious-activity.title",checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.ncaRegistered.title",checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.riskassessment.policy.title",
        checkElementTextIncludes(_, "businessactivities.RiskAssessmentType.lbl.01", "businessactivities.RiskAssessmentType.lbl.02")),
      ("businessactivities.accountantForAMLSRegulations.title",checkElementTextIncludes(_, "lbl.yes")),
      ("businessactivities.whoisyouraccountant.title",
        checkElementTextIncludes(_, "AccountantName","tradingName","line1","line2","line3","line4","AB12CD")),
      ("businessactivities.tax.matters.title",checkElementTextIncludes(_, "lbl.yes"))
    )

    "include the provided data" in new ViewFixture {

      def view = views.html.businessactivities.summary(
        EmptyForm,
        BusinessActivities(
          Some(InvolvedInOtherYes("OtherActivities")),
          Some(ExpectedBusinessTurnover.First),
          Some(ExpectedAMLSTurnover.First),
          Some(BusinessFranchiseYes("FranchiseName")),
          Some(KeepTransactionRecordYes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("SoftwareName")))),
          None, // This is only present in renewal
          Some(NCARegistered(true)),
          Some(AccountantForAMLSRegulations(true)),
          Some(IdentifySuspiciousActivity(true)),
          Some(RiskAssessmentPolicyYes(Set(PaperBased, Digital))),
          Some(HowManyEmployees(Some("123"), Some("456"))),
          Some(WhoIsYourAccountant("AccountantName",Some("tradingName"),UkAccountantsAddress("line1","line2",Some("line3"),Some("line4"),"AB12CD"))),
          Some(TaxMatters(true))
        ),
        None,
        true
      )

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")

        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be (None)
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }
      }
    }
  }
}