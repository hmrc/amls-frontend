/*
 * Copyright 2021 HM Revenue & Customs
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
import models.businessmatching._
import org.jsoup.nodes.Element
import org.scalatest.MustMatchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import forms.EmptyForm
import org.mockito.Mockito.when
import views.html.businessmatching.summary

import scala.collection.JavaConversions._

class summarySpec extends AmlsViewSpec with MustMatchers with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    lazy val summary = app.injector.instanceOf[summary]
    implicit val requestWithToken = addTokenForView()

    val defaultActivitiesUrl = controllers.businessmatching.routes.RegisterServicesController.get().url
  }

  "businessmatching view" must {
    "have correct title when presubmission" in new ViewFixture {

      def view = summary(EmptyForm, BusinessMatching(), defaultActivitiesUrl, true)

      doc.title must startWith("Check your answers before starting your application" + " - " + "Pre-application")
      heading.html must be("Check your answers before starting your application")
      subHeading.html must include("Pre-application")

    }

    "have correct title when not presubmission" in new ViewFixture {

      def view = summary(EmptyForm, BusinessMatching(), defaultActivitiesUrl, false)

      doc.title must startWith("Check your answers" + " - " + "Update information")
      heading.html must be("Check your answers")
      subHeading.html must include("Update information")
    }

    def checkElementTextIncludes(el:Element, keys : String*) = {
      val t = el.text()
      keys.foreach { k =>
        t must include (Messages(k))
      }
      true
    }

    def checkListContainsItems(parent:Element, keysToFind:Set[String]) = {
      val texts = parent.select("li").toSet.map((el:Element) => el.text())
      texts must be (keysToFind.map(k => Messages(k)))
      true
    }

    "include the provided data when MoneyServicesBusiness and TransmittingMoney were selected for a Limited Company" in new ViewFixture {

      val msbServices = BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, ForeignExchange))
      val BusinessActivitiesModel = BusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, HighValueDealing, MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      val BusinessActivitiesWithouMSB = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
      val businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("AB1 2CD"), Country("United Kingdom", "GB"))
      val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany), businessAddress, "XE0000000000000")
      val TypeOfBusinessModel = TypeOfBusiness("test")
      val CompanyRegistrationNumberModel = CompanyRegistrationNumber("12345678")
      val BusinessAppliedForPSRNumberModel = BusinessAppliedForPSRNumberYes("123456")

      val testBusinessMatching = BusinessMatching(
        Some(ReviewDetailsModel),
        Some(BusinessActivitiesModel),
        Some(msbServices),
        Some(TypeOfBusinessModel),
        Some(CompanyRegistrationNumberModel),
        Some(BusinessAppliedForPSRNumberModel))

      def view = summary(EmptyForm, testBusinessMatching, defaultActivitiesUrl)

      val sectionChecks = Table[String, Element => Boolean, String](
        ("title key", "check", "editLink"),
        ("businessmatching.summary.business.address.lbl", checkElementTextIncludes(_, "line1", "line2", "line3", "line4", "AB1 2CD", "United Kingdom"), ""),
        ("businessmatching.registrationnumber.title", checkElementTextIncludes(_, "12345678"),
          controllers.businessmatching.routes.CompanyRegistrationNumberController.get(true).toString),
        ("businessmatching.registerservices.title", checkListContainsItems(_, Set(
          "businessmatching.registerservices.servicename.lbl.01",
          "businessmatching.registerservices.servicename.lbl.03",
          "businessmatching.registerservices.servicename.lbl.04",
          "businessmatching.registerservices.servicename.lbl.05",
          "businessmatching.registerservices.servicename.lbl.06",
          "businessmatching.registerservices.servicename.lbl.07",
          "businessmatching.registerservices.servicename.lbl.08")),
          defaultActivitiesUrl),
        ("businessmatching.services.title", checkListContainsItems(_, Set(
          "businessmatching.services.list.lbl.01",
          "businessmatching.services.list.lbl.02",
          "businessmatching.services.list.lbl.03",
          "businessmatching.services.list.lbl.04",
          "businessmatching.services.list.lbl.05")),
          controllers.businessmatching.routes.MsbSubSectorsController.get(true).toString),
        ("businessmatching.psr.number.title", checkElementTextIncludes(_, "123456"),
          controllers.businessmatching.routes.PSRNumberController.get(true).toString)
      )

      html must not include Messages("businessmatching.typeofbusiness.title")

      html must not include Messages("button.logout")
      html must include(Messages("businessmatching.button.confirm.start"))

      val sections = doc.getElementsByTag("section").zipWithIndex

      for((section, index) <- sections) {
        val (key, check, editLink) = sectionChecks(index)
        section.select("h2").text() must be(Messages(key))
        check(section) must be(true)
        section.select("a[href]").attr("href") must be(editLink)
      }
    }

    "include the provided data for an UnincorporatedBody with BusinessAppliedForPSRNumberNo" in new ViewFixture {

      val msbServices = BusinessMatchingMsbServices(Set(CurrencyExchange, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))
      val BusinessActivitiesModel = BusinessActivities(Set(
        AccountancyServices,
        BillPaymentServices,
        EstateAgentBusinessService,
        HighValueDealing,
        TrustAndCompanyServices,
        TelephonePaymentService))
      val BusinessActivitiesWithouMSB = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
      val businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("AB1 2CD"), Country("United Kingdom", "GB"))
      val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.UnincorporatedBody), businessAddress, "XE0000000000000")
      val TypeOfBusinessModel = TypeOfBusiness("test")
      val CompanyRegistrationNumberModel = CompanyRegistrationNumber("12345678")
      val BusinessAppliedForPSRNumberModel = BusinessAppliedForPSRNumberNo

      val testBusinessMatching = BusinessMatching(
        Some(ReviewDetailsModel),
        Some(BusinessActivitiesModel),
        Some(msbServices),
        Some(TypeOfBusinessModel),
        Some(CompanyRegistrationNumberModel),
        Some(BusinessAppliedForPSRNumberModel))

      def view = summary(EmptyForm, testBusinessMatching, defaultActivitiesUrl)

      val sectionChecks = Table[String, Element => Boolean, String](
        ("title key", "check", "editLink"),
        ("businessmatching.summary.business.address.lbl", checkElementTextIncludes(_, "line1", "line2", "line3", "line4", "AB1 2CD", "United Kingdom"), ""),
        ("businessmatching.typeofbusiness.title", checkElementTextIncludes(_, "test"),
          controllers.businessmatching.routes.TypeOfBusinessController.get(true).toString),
        ("businessmatching.registerservices.title", checkListContainsItems(_, Set(
          "businessmatching.registerservices.servicename.lbl.01",
          "businessmatching.registerservices.servicename.lbl.03",
          "businessmatching.registerservices.servicename.lbl.04",
          "businessmatching.registerservices.servicename.lbl.05",
          "businessmatching.registerservices.servicename.lbl.07",
          "businessmatching.registerservices.servicename.lbl.08")),
          defaultActivitiesUrl
        )
      )

      html must not include Messages("businessmatching.services.title")
      html must not include Messages("businessmatching.registrationnumber.title")

      html must include(Messages("button.logout"))
      html must not include Messages("businessmatching.button.confirm.start")
      val sections = doc.getElementsByTag("section").zipWithIndex

      for((section, index) <- sections) {
        val (key, check, editLink) = sectionChecks(index)
        section.select("h2").text() must be(Messages(key))
        check(section) must be(true)
        section.select("a[href]").attr("href") must be(editLink)
      }
    }
  }
}