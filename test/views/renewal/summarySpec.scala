package views.renewal

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, AccountancyServices, BusinessActivities}
import models.renewal.{AMLSTurnover, BusinessTurnover, InvolvedInOtherYes, Renewal}
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

    val fullProductSet = Set("hvd.products.option.01","hvd.products.option.02","hvd.products.option.03",
      "hvd.products.option.04","hvd.products.option.05","hvd.products.option.06","hvd.products.option.07",
      "hvd.products.option.08","hvd.products.option.09","hvd.products.option.10","hvd.products.option.11",
      "Other Product"
    )

    val sectionChecks = Table[String, Element=>Boolean](
      ("title key", "check"),
      ("hvd.cash.payment.title",checkElementTextIncludes(_, "lbl.yes", "20 June 2012")),
      ("hvd.products.title", checkListContainsItems(_, fullProductSet)),
      ("hvd.excise.goods.title", checkElementTextIncludes(_, "lbl.yes"))
    )

    "include the provided data" in new ViewFixture {
      def view = {
        val renewalModel = Renewal(
          Some(InvolvedInOtherYes("test")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          false
        )

        val businessActivitiesModel = BusinessActivities(
          Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)
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
