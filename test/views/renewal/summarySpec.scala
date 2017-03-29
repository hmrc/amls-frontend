package views.renewal

import models.Country
import models.businessmatching._
import models.renewal._
import org.scalatest.MustMatchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

class summarySpec extends GenericTestHelper with MustMatchers  with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {
    "have correct title" in new ViewFixture {


      def view = views.html.renewal.summary(Renewal(), None)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.renewal.summary(Renewal(), None)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.renewal"))
    }

    def checkListContainsItems(parent:Element, keysToFind:Set[String]) = {
      val texts = parent.select("li").toSet.map((el:Element) => el.text())
      texts must be (keysToFind.map(k => Messages(k)))
      true
    }

    def checkElementTextIncludes(el:Element, keys : String*) = {
      val t = el.text()
      keys.foreach { k =>
        t must include (Messages(k))
      }
      true
    }

    val fullActivitiesSet = Set(
      "businessmatching.registerservices.servicename.lbl.01",
      "businessmatching.registerservices.servicename.lbl.02",
      "businessmatching.registerservices.servicename.lbl.03",
      "businessmatching.registerservices.servicename.lbl.04",
      "businessmatching.registerservices.servicename.lbl.05",
      "businessmatching.registerservices.servicename.lbl.06",
      "businessmatching.registerservices.servicename.lbl.07"
    )

    val sectionChecks = Table[String, Element=>Boolean](
      ("title key", "check"),
      ("renewal.involvedinother.title",checkElementTextIncludes(_, "test text")),
      ("renewal.business-turnover.title", checkElementTextIncludes(_, "£0 to £14,999")),
      ("renewal.turnover.title", checkElementTextIncludes(_, "£0 to £14,999")),
      ("renewal.turnover.title", checkListContainsItems(_, fullActivitiesSet)),
      ("renewal.customer.outside.uk.title", checkElementTextIncludes(_, "United Kingdom"))
    )

    "include the provided data" in new ViewFixture {
      def view = {
        val renewalModel = Renewal(
          Some(InvolvedInOtherYes("test text")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          Some(PercentageOfCashPaymentOver15000.First),
          false
        )

        val businessActivitiesModel = BusinessActivities(
          Set(AccountancyServices,
            BillPaymentServices,
            EstateAgentBusinessService,
            HighValueDealing,
            MoneyServiceBusiness,
            TrustAndCompanyServices,
            TelephonePaymentService)
        )

        views.html.renewal.summary(renewalModel, Some(businessActivitiesModel))
      }

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be (None)
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }}
    }
  }
}
