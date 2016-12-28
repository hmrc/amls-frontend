package views.bankdetails

import models.bankdetails.{BankAccount, BankDetails, PersonalAccount, UKAccount}
import models.hvd._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import views.ViewFixture

import scala.collection.JavaConversions._


class summarySpec extends WordSpec with MustMatchers with OneAppPerSuite with TableDrivenPropertyChecks {

  "summary view" when {
    "section in incomplete" must {
      "have correct title" in new ViewFixture {

        def view = views.html.bankdetails.summary(Seq(BankDetails()), false, true, true)

        doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.bankdetails"))
      }

      "have correct headings" in new ViewFixture {
        def view = views.html.bankdetails.summary(Seq(BankDetails()), false, true, true)

        heading.html must be(Messages("title.cya"))
        subHeading.html must include(Messages("summary.bankdetails"))
      }

      "have correct button text" in new ViewFixture {
        def view = views.html.bankdetails.summary(Seq(BankDetails()), false, true, true)

        doc.getElementsByClass("button").html must include(Messages("button.summary.acceptandcomplete"))
      }
    }

    "section in complete" must {
      "have correct title" in new ViewFixture {

        def view = views.html.bankdetails.summary(Seq(BankDetails()), true, true, true)

        doc.title must startWith(Messages("title.ya") + " - " + Messages("summary.bankdetails"))
      }

      "have correct headings" in new ViewFixture {
        def view = views.html.bankdetails.summary(Seq(BankDetails()), true, true, true)

        heading.html must be(Messages("title.ya"))
        subHeading.html must include(Messages("summary.bankdetails"))
      }

      "have correct button text" in new ViewFixture {
        def view = views.html.bankdetails.summary(Seq(BankDetails()), true, true, true)

        doc.getElementsByClass("button").html must include(Messages("button.confirmandcontinue"))
      }
    }
  }

  it must {
    "include the provided data for a UK bank Account" in new ViewFixture {

      def checkListContainsItems(parent: Element, keysToFind: Set[String]) = {
        val texts = parent.select("li").toSet.map((el: Element) => el.text())
        texts must be(keysToFind.map(k => Messages(k)))
        true
      }

      def checkElementTextIncludes(el: Element, keys: String*) = {
        val t = el.text()
        keys.foreach { k =>
          t must include(Messages(k))
        }
        true
      }

      val fullProductSet = Set("hvd.products.option.01", "hvd.products.option.02", "hvd.products.option.03",
        "hvd.products.option.04", "hvd.products.option.05", "hvd.products.option.06", "hvd.products.option.07",
        "hvd.products.option.08", "hvd.products.option.09", "hvd.products.option.10", "hvd.products.option.11",
        "Other Product"
      )

      val testUKBankDetails = BankDetails(Some(PersonalAccount), Some(BankAccount("Account Name", UKAccount("1234567890", "123456"))))
      private val title = Messages("bankdetails.bankaccount.accountname") + ": " + "Account Name"
      val sectionCheckstestUKBankDetails = Table[String, Element => Boolean](
        ("title key", "check"),
//        (title,checkElementTextIncludes(_, "lbl.yes", "20 June 2012")),
        (title, checkListContainsItems(_, Set(Messages("bankdetails.bankaccount.sortcode"))))
      )

      def view = {

        val testdata = Seq(testUKBankDetails)

        views.html.bankdetails.summary(testdata, true, true, true)
      }

      forAll(sectionCheckstestUKBankDetails) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))
        val hTwoa = hTwos.toList.find(e => e.text() == title)

        println("******************" + hTwoa)
        html must be ("hadvbfawe")
        hTwoa must not be (None)
//        hTwo must not be (None)
//        val section = hTwo.get.parents().select("section").first()
//        check(section) must be(true)
      }
      }
    }
  }
}