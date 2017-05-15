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

package views.businessmatching

import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching._
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._


class businessmatchingSpec extends GenericTestHelper
  with MustMatchers
  with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "businessmatching view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.businessmatching.summary(BusinessMatching())

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.businessmatching"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.businessmatching.summary(BusinessMatching())

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.businessmatching"))

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

      val msbServices = MsbServices(Set(TransmittingMoney, CurrencyExchange, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))
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

      def view = views.html.businessmatching.summary(testBusinessMatching)

      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        ("businessmatching.summary.business.address.lbl", checkElementTextIncludes(_, "line1", "line2", "line3", "line4", "AB1 2CD", "United Kingdom")),
        ("businessmatching.registrationnumber.title", checkElementTextIncludes(_, "12345678")),
        ("businessmatching.registerservices.title", checkListContainsItems(_, Set(
          "businessmatching.registerservices.servicename.lbl.01",
          "businessmatching.registerservices.servicename.lbl.02",
          "businessmatching.registerservices.servicename.lbl.03",
          "businessmatching.registerservices.servicename.lbl.04",
          "businessmatching.registerservices.servicename.lbl.05",
          "businessmatching.registerservices.servicename.lbl.06",
          "businessmatching.registerservices.servicename.lbl.07"))),
        ("businessmatching.services.title", checkListContainsItems(_, Set(
          "businessmatching.services.list.lbl.01",
          "businessmatching.services.list.lbl.02",
          "businessmatching.services.list.lbl.03",
          "businessmatching.services.list.lbl.04"))),
        ("businessmatching.psr.number.title", checkElementTextIncludes(_, "123456"))
      )

      html must not include Messages("businessmatching.typeofbusiness.title")

      html must not include Messages("button.logout")
      html must include(Messages("businessmatching.button.confirm.start"))


      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be None
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }
      }
    }

    "include the provided data for an UnincorporatedBody with BusinessAppliedForPSRNumberNo" in new ViewFixture {

      val msbServices = MsbServices(Set(CurrencyExchange, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))
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

      def view = views.html.businessmatching.summary(testBusinessMatching)

      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        ("businessmatching.summary.business.address.lbl", checkElementTextIncludes(_, "line1", "line2", "line3", "line4", "AB1 2CD", "United Kingdom")),
        ("businessmatching.typeofbusiness.title", checkElementTextIncludes(_, "test")),
        ("businessmatching.registerservices.title", checkListContainsItems(_, Set(
          "businessmatching.registerservices.servicename.lbl.01",
          "businessmatching.registerservices.servicename.lbl.02",
          "businessmatching.registerservices.servicename.lbl.03",
          "businessmatching.registerservices.servicename.lbl.04",
          "businessmatching.registerservices.servicename.lbl.06",
          "businessmatching.registerservices.servicename.lbl.07")))
      )

      html must not include Messages("businessmatching.services.title")
      html must not include Messages("businessmatching.registrationnumber.title")

      html must include(Messages("button.logout"))
      html must not include Messages("businessmatching.button.confirm.start")


      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be None
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }
      }
    }
  }
}