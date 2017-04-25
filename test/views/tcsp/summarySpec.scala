package views.tcsp

import models.tcsp._
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.MustMatchers
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.{Fixture, HtmlAssertions}
import scala.collection.JavaConversions._


class summarySpec extends GenericTestHelper
                  with MustMatchers
                  with HtmlAssertions
                  with TableDrivenPropertyChecks{

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

    val sectionChecks = Table[String, Element=>Boolean](
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
      ("tcsp.servicesOfAnotherTcsp.title", checkElementTextIncludes(_, "Money Laundering Regulation reference number: 789oinhytrd4567"))
    )

    "include the provided data" in new ViewFixture {
      def view = {
        val testdata = Tcsp(
          Some(TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, RegisteredOfficeEtc, CompanyDirectorEtc, CompanyFormationAgent(true,false)))),
          Some(ProvidedServices(Set(PhonecallHandling,EmailHandling,EmailServer,
                                    SelfCollectMailboxes,MailForwarding,Receptionist,ConferenceRooms, Other("sfasfasef")))),
          Some(ServicesOfAnotherTCSPYes("789oinhytrd4567"))
        )

        views.html.tcsp.summary(testdata)
      }

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be (None)
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }}

      doc.getElementsMatchingOwnText(Messages("tcsp.off-the-shelf.companies.lbl")).first().nextElementSibling().nextElementSibling().text() must be("Yes")
      doc.getElementsMatchingOwnText(Messages("tcsp.create.complex.corporate.structures.lbl")).first().nextElementSibling().nextElementSibling().text() must be("No")
    }


  }
}