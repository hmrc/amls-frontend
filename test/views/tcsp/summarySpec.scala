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

package views.tcsp

import generators.AmlsReferenceNumberGenerator
import models.tcsp._
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

class summarySpec extends GenericTestHelper with MustMatchers with HtmlAssertions with TableDrivenPropertyChecks with AmlsReferenceNumberGenerator {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {
    "have correct title, heading and subheading" in new ViewFixture {

      def view = views.html.tcsp.summary(Tcsp())

      val title = Messages("title.cya") + " - " + Messages("summary.tcsp") + " - " +
                  Messages("title.amls") + " - " + Messages("title.gov")

      doc.title must be(title)
      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.tcsp"))
    }

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      ("tcsp.kind.of.service.provider.title", checkListContainsItems(_, Set("tcsp.service.provider.lbl.01",
                                                                            "tcsp.service.provider.lbl.02",
                                                                            "tcsp.service.provider.lbl.03",
                                                                            "tcsp.service.provider.lbl.04",
                                                                            "tcsp.service.provider.lbl.05"))),
      ("tcsp.provided_services.title", checkListContainsItems(_, Set("tcsp.provided_services.service.lbl.01",
                                                                     "tcsp.provided_services.service.lbl.02",
                                                                     "tcsp.provided_services.service.lbl.03",
                                                                     "tcsp.provided_services.service.lbl.04",
                                                                     "tcsp.provided_services.service.lbl.05",
                                                                     "tcsp.provided_services.service.lbl.06",
                                                                     "tcsp.provided_services.service.lbl.07",
                                                                     "Other:sfasfasef"))),
      ("tcsp.servicesOfAnotherTcsp.title", checkElementTextIncludes(_, "lbl.yes")),
      ("tcsp.anothertcspsupervision.title", checkElementTextIncludes(_, s"Money Laundering Regulation reference number: $amlsRegistrationNumber"))
    )

    "include the provided data" in new ViewFixture {
      def view = {
        val testdata = Tcsp(
          Some(TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, RegisteredOfficeEtc, CompanyDirectorEtc, CompanyFormationAgent(true,false)))),
          Some(ProvidedServices(Set(
            PhonecallHandling,EmailHandling,EmailServer,SelfCollectMailboxes,MailForwarding,Receptionist,ConferenceRooms, Other("sfasfasef")
          ))),
          Some(true),
          Some(ServicesOfAnotherTCSPYes(amlsRegistrationNumber))
        )

        views.html.tcsp.summary(testdata)
      }

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be None
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }}

      doc.getElementsMatchingOwnText(Messages("tcsp.off-the-shelf.companies.lbl")).first().nextElementSibling().nextElementSibling().text() must be("Yes")
      doc.getElementsMatchingOwnText(Messages("tcsp.create.complex.corporate.structures.lbl")).first().nextElementSibling().nextElementSibling().text() must be("No")
    }

  }
}