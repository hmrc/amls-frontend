/*
 * Copyright 2022 HM Revenue & Customs
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

package views.tcsp

import generators.AmlsReferenceNumberGenerator
import models.tcsp._
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import views.Fixture
import views.html.tcsp.summary

import scala.collection.JavaConversions._

class summarySpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks with AmlsReferenceNumberGenerator {

  trait ViewFixture extends Fixture {
    lazy val summary = app.injector.instanceOf[summary]
    implicit val requestWithToken = addTokenForView(FakeRequest())
  }

  "summary view" must {
    "have correct title, heading and subheading" in new ViewFixture {

      def view = summary(Tcsp(), List())

      val title = Messages("title.cya") + " - " + Messages("summary.tcsp") + " - " +
                  Messages("title.amls") + " - " + Messages("title.gov")

      doc.title must be(title)
      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.tcsp"))
    }

    "include the provided data" in new ViewFixture {

      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        ("tcsp.kind.of.service.provider.title", checkListContainsItems(_, Set("tcsp.service.provider.lbl.01",
          "tcsp.service.provider.lbl.02",
          "tcsp.service.provider.lbl.03",
          "tcsp.service.provider.lbl.04",
          "tcsp.service.provider.lbl.05"))),

        ("tcsp.off-the-shelf.companies.lbl", checkElementTextIncludes(_, "lbl.yes")),
        ("tcsp.create.complex.corporate.structures.lbl", checkElementTextIncludes(_, "lbl.no")),

        ("tcsp.provided_services.title", checkListContainsItems(_, Set("tcsp.provided_services.service.lbl.01",
          "tcsp.provided_services.service.lbl.02",
          "tcsp.provided_services.service.lbl.03",
          "tcsp.provided_services.service.lbl.04",
          "tcsp.provided_services.service.lbl.05",
          "tcsp.provided_services.service.lbl.06",
          "tcsp.provided_services.service.lbl.07",
          "sfasfasef"))),
        ("tcsp.servicesOfAnotherTcsp.title", checkElementTextIncludes(_, "lbl.yes")),
        ("tcsp.anothertcspsupervision.cya.additional.header", checkElementTextIncludes(_, s"$amlsRegistrationNumber"))
      )

      def view = {
        val testdata = Tcsp(
          Some(TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, RegisteredOfficeEtc, CompanyDirectorEtc, CompanyFormationAgent))),
            Some(OnlyOffTheShelfCompsSoldYes),
            Some(ComplexCorpStructureCreationNo),
            Some(ProvidedServices(Set(
              PhonecallHandling,EmailHandling,EmailServer,SelfCollectMailboxes,MailForwarding,Receptionist,ConferenceRooms, Other("sfasfasef")
            ))),
            Some(true),
            Some(ServicesOfAnotherTCSPYes(amlsRegistrationNumber))
          )

        val sortedList = List(
          "Registered office, business address, or virtual office services provider",
          "Trustee provider",
          "Company director, secretary, or partner provider",
          "Trust or company formation agent",
          "Nominee shareholders provider"
        )
        summary(testdata, sortedList)
      }

      forAll(sectionChecks) { (key, check) => {
        val questions = doc.select("span.bold")

        val question = questions.toList.find(e => e.text() == Messages(key))

        question must not be None
        val section = question.get.parents().select("div").first()
        check(section) must be(true)
      }}

      doc.select("span.bold").toList.find(e => e.text() ==  Messages("tcsp.off-the-shelf.companies.lbl"))
        .get.parents().select("div").first().getElementsByClass("cya-summary-list__value").html() must be("Yes")

      doc.select("span.bold").toList.find(e => e.text() ==  Messages("tcsp.create.complex.corporate.structures.lbl"))
        .get.parents().select("div").first().getElementsByClass("cya-summary-list__value").html() must be("No")
    }

  }
}