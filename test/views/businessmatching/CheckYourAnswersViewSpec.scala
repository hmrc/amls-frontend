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

package views.businessmatching

import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.businessmatching._
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Request}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.AmlsViewSpec
import utils.businessmatching.CheckYourAnswersHelper
import views.Fixture
import views.html.businessmatching.CheckYourAnswersView

import scala.jdk.CollectionConverters._

class CheckYourAnswersViewSpec extends AmlsViewSpec with Matchers with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    lazy val checkYourAnswersView                                  = app.injector.instanceOf[CheckYourAnswersView]
    lazy val cyaHelper                                             = app.injector.instanceOf[CheckYourAnswersHelper]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

    val defaultActivitiesUrl = controllers.businessmatching.routes.RegisterServicesController.get().url
  }

  "businessmatching view" must {
    "have correct title when presubmission" in new ViewFixture {

      def view = checkYourAnswersView(SummaryList(Seq.empty), None, true)

      doc.title       must startWith("Check your answers before starting your application" + " - " + "Pre-application")
      heading.html    must be("Check your answers before starting your application")
      subHeading.html must include("Pre-application")

    }

    "have correct title when not presubmission" in new ViewFixture {

      def view = checkYourAnswersView(SummaryList(Seq.empty), None, false)

      doc.title       must startWith("Check your answers" + " - " + "Update information")
      heading.html    must be("Check your answers")
      subHeading.html must include("Update information")
    }

    def checkElementTextIncludes(el: Element, keys: String*) = {
      val t = el.text()
      keys.foreach { k =>
        t must include(messages(k))
      }
      true
    }

    def checkListContainsItems(parent: Element, keysToFind: Set[String]) = {
      val texts = parent.select("li").asScala.map((el: Element) => el.text())
      texts must be(keysToFind.map(k => messages(k)))
      true
    }

    "include the provided data when MoneyServicesBusiness and TransmittingMoney were selected for a Limited Company" in new ViewFixture {

      val msbServices                      = BusinessMatchingMsbServices(
        Set(TransmittingMoney, CurrencyExchange, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, ForeignExchange)
      )
      val BusinessActivitiesModel          = BusinessActivities(
        Set(
          AccountancyServices,
          BillPaymentServices,
          EstateAgentBusinessService,
          HighValueDealing,
          MoneyServiceBusiness,
          TrustAndCompanyServices,
          TelephonePaymentService
        )
      )
      val businessAddress                  =
        Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AB1 2CD"), Country("United Kingdom", "GB"))
      val ReviewDetailsModel               =
        ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany), businessAddress, "XE0000000000000")
      val TypeOfBusinessModel              = TypeOfBusiness("test")
      val CompanyRegistrationNumberModel   = CompanyRegistrationNumber("12345678")
      val BusinessAppliedForPSRNumberModel = BusinessAppliedForPSRNumberYes("123456")

      val testBusinessMatching = BusinessMatching(
        Some(ReviewDetailsModel),
        Some(BusinessActivitiesModel),
        Some(msbServices),
        Some(TypeOfBusinessModel),
        Some(CompanyRegistrationNumberModel),
        Some(BusinessAppliedForPSRNumberModel)
      )

      val isPreSubmission = true

      val summaryList  = cyaHelper.createSummaryList(testBusinessMatching, isPreSubmission, isPending = false)
      val submitButton = cyaHelper.getSubmitButton(
        testBusinessMatching.businessAppliedForPSRNumber,
        isPreSubmission,
        testBusinessMatching.preAppComplete
      )

      def view = checkYourAnswersView(summaryList, submitButton, isPreSubmission)

      val sectionChecks = Table[String, Element => Boolean, String](
        ("title key", "check", "editLink"),
        (
          "businessmatching.summary.business.address.lbl",
          checkElementTextIncludes(_, "line1", "line2", "line3", "line4", "AB1 2CD", "United Kingdom"),
          ""
        ),
        (
          "businessmatching.registrationnumber.title",
          checkElementTextIncludes(_, "12345678"),
          controllers.businessmatching.routes.CompanyRegistrationNumberController.get(true).toString
        ),
        (
          "businessmatching.registerservices.title",
          checkListContainsItems(
            _,
            Set(
              "businessmatching.registerservices.servicename.lbl.01",
              "businessmatching.registerservices.servicename.lbl.03",
              "businessmatching.registerservices.servicename.lbl.04",
              "businessmatching.registerservices.servicename.lbl.05",
              "businessmatching.registerservices.servicename.lbl.06",
              "businessmatching.registerservices.servicename.lbl.07",
              "businessmatching.registerservices.servicename.lbl.08"
            )
          ),
          defaultActivitiesUrl
        ),
        (
          "businessmatching.services.title",
          checkListContainsItems(
            _,
            Set(
              "businessmatching.services.list.lbl.01",
              "businessmatching.services.list.lbl.02",
              "businessmatching.services.list.lbl.03",
              "businessmatching.services.list.lbl.04",
              "businessmatching.services.list.lbl.05"
            )
          ),
          controllers.businessmatching.routes.MsbSubSectorsController.get(true).toString
        ),
        (
          "businessmatching.psr.number.title",
          checkElementTextIncludes(_, "123456"),
          controllers.businessmatching.routes.PSRNumberController.get(true).toString
        )
      )

      html must not include messages("businessmatching.typeofbusiness.title")

      html must not include messages("button.logout")
      html must include(messages("businessmatching.button.confirm.start"))

      val sections = doc.getElementsByTag("section").asScala.zipWithIndex

      for ((section, index) <- sections) {
        val (key, check, editLink) = sectionChecks(index)
        section.select("h2").text()            must be(messages(key))
        check(section)                         must be(true)
        section.select("a[href]").attr("href") must be(editLink)
      }
    }

    "include the provided data for an UnincorporatedBody with No MSB Services" in new ViewFixture {

      val businessActivitiesWithoutMSB   = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
      val businessAddress                =
        Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AB1 2CD"), Country("United Kingdom", "GB"))
      val ReviewDetailsModel             =
        ReviewDetails("BusinessName", Some(BusinessType.UnincorporatedBody), businessAddress, "XE0000000000000")
      val TypeOfBusinessModel            = TypeOfBusiness("test")
      val CompanyRegistrationNumberModel = CompanyRegistrationNumber("12345678")

      val testBusinessMatching = BusinessMatching(
        Some(ReviewDetailsModel),
        Some(businessActivitiesWithoutMSB),
        None,
        Some(TypeOfBusinessModel),
        Some(CompanyRegistrationNumberModel),
        Some(BusinessAppliedForPSRNumberNo)
      )

      val isPreSubmission = true

      val summaryList  = cyaHelper.createSummaryList(testBusinessMatching, isPreSubmission, isPending = false)
      val submitButton = cyaHelper.getSubmitButton(
        testBusinessMatching.businessAppliedForPSRNumber,
        isPreSubmission,
        testBusinessMatching.preAppComplete
      )

      def view = checkYourAnswersView(summaryList, submitButton, isPreSubmission)

      val sectionChecks = Table[String, Element => Boolean, String](
        ("title key", "check", "editLink"),
        (
          "businessmatching.summary.business.address.lbl",
          checkElementTextIncludes(_, "line1", "line2", "line3", "line4", "AB1 2CD", "United Kingdom"),
          ""
        ),
        (
          "businessmatching.typeofbusiness.title",
          checkElementTextIncludes(_, "test"),
          controllers.businessmatching.routes.TypeOfBusinessController.get(true).toString
        ),
        (
          "businessmatching.registerservices.title",
          checkListContainsItems(
            _,
            Set(
              s"businessmatching.registerservices.servicename.lbl.${TrustAndCompanyServices.value}",
              s"businessmatching.registerservices.servicename.lbl.${TelephonePaymentService.value}"
            )
          ),
          defaultActivitiesUrl
        )
      )

      html must not include messages("businessmatching.services.title")
      html must not include messages("businessmatching.registrationnumber.title")

      html must include(messages("button.logout"))
      html must not include messages("businessmatching.button.confirm.start")
      val sections = doc.getElementsByTag("section").asScala.zipWithIndex

      for ((section, index) <- sections) {
        val (key, check, editLink) = sectionChecks(index)
        section.select("h2").text()            must be(messages(key))
        check(section)                         must be(true)
        section.select("a[href]").attr("href") must be(editLink)
      }
    }
  }
}
