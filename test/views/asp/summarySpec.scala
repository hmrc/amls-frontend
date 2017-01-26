package views.asp

import models.asp._
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.{HtmlAssertions, ViewFixture}
import scala.collection.JavaConversions._



class summarySpec extends GenericTestHelper
        with MustMatchers

        with HtmlAssertions
        with TableDrivenPropertyChecks {

  "summary view" must {
    "have correct title" in new ViewFixture {


      def view = views.html.asp.summary(Asp())

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.asp.summary(Asp())

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.asp"))
    }

    val sectionChecks = Table[String, Element=>Boolean](
      ("title key", "check"),
      ("asp.services.title", checkListContainsItems(_, Set("asp.service.lbl.01",
                                                            "asp.service.lbl.02",
                                                            "asp.service.lbl.03",
                                                            "asp.service.lbl.04",
                                                            "asp.service.lbl.05"))),
      ("asp.other.business.tax.matters.title", checkElementTextIncludes(_, "lbl.yes"))
    )

    "include the provided data" in new ViewFixture {
      def view = {
        val testdata = Asp(
          Some(ServicesOfBusiness(Set(Accountancy, PayrollServices, BookKeeping, Auditing, FinancialOrTaxAdvice))),
          Some(OtherBusinessTaxMattersYes)
        )

        views.html.asp.summary(testdata)
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
