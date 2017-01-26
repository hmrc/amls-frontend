package views.bankdetails

import models.bankdetails._
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.ViewFixture

import scala.collection.JavaConversions._


class summarySpec extends GenericTestHelper with MustMatchers  with TableDrivenPropertyChecks {

  "summary view" when {
    "section is incomplete" must {
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

    "section is complete" must {
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

  def checkListContainsItems(parent: Element, keysToFind: Set[String]) = {
    val texts = parent.select("li").toSet.map((el: Element) => el.text())

    texts.tail must be(keysToFind.map(k => Messages(k)))
    true
  }

  def checkElementTextIncludes(el: Element, keys: String*) = {
    val t = el.text()
    keys.foreach { k =>
      t must include(Messages(k))
    }
    true
  }

  it must {

    "include the provided data for a UKAccount" in new ViewFixture {

      private val title = Messages("bankdetails.bankaccount.accountname") + ": " + "Account Name"

      private val bankDetailsSet = Set(
        Messages("bankdetails.bankaccount.accountnumber") + ": 1234567890",
        Messages("bankdetails.bankaccount.sortcode") + ": 12-34-56",
        Messages("bankdetails.bankaccount.accounttype.uk.lbl") + ": " + Messages("lbl.yes"),
        Messages("bankdetails.bankaccount.accounttype.lbl") + ": " + Messages("bankdetails.summary.accounttype.lbl.01")
      )

      val sectionCheckstestUKBankDetails = Table[String, Element => Boolean](
        ("title key", "check"),
        (title, checkElementTextIncludes(_,
          "12-34-56",
          "1234567890",
          "bankdetails.bankaccount.accounttype.uk.lbl", "lbl.yes",
          "bankdetails.bankaccount.accounttype.lbl", "bankdetails.summary.accounttype.lbl.01")
          ), (title, checkListContainsItems(_, bankDetailsSet))
      )

      def view = {
        val testdata = Seq(BankDetails(Some(PersonalAccount), Some(BankAccount("Account Name", UKAccount("1234567890", "123456")))))

        views.html.bankdetails.summary(testdata, true, true, true)
      }

      forAll(sectionCheckstestUKBankDetails) { (key, check) => {
        val hTwos = doc.select("li.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == title)

        hTwo must not be (None)
        val section = hTwo.get.parents().select("li").first()
        check(section) must be(true)
      }
      }
    }

    "include the provided data for a NonUKAccountNumber" in new ViewFixture {

      private val title = Messages("bankdetails.bankaccount.accountname") + ": " + "Account Name"

      private val bankDetailsSet = Set(
        Messages("bankdetails.bankaccount.accountnumber") + ": 56789",
        Messages("bankdetails.bankaccount.accounttype.uk.lbl") + ": " + Messages("lbl.no"),
        Messages("bankdetails.bankaccount.accounttype.lbl") + ": " + Messages("bankdetails.summary.accounttype.lbl.01")
      )

      val sectionCheckstestUKBankDetails = Table[String, Element => Boolean](
        ("title key", "check"),
        (title, checkElementTextIncludes(_,
          "56789",
          "bankdetails.bankaccount.accounttype.uk.lbl", "lbl.no",
          "bankdetails.bankaccount.accounttype.lbl", "bankdetails.summary.accounttype.lbl.01")
          ), (title, checkListContainsItems(_, bankDetailsSet))
      )

      def view = {
        val testdata = Seq(BankDetails(Some(PersonalAccount), Some(BankAccount("Account Name", NonUKAccountNumber("56789")))))

        views.html.bankdetails.summary(testdata, true, true, true)
      }

      forAll(sectionCheckstestUKBankDetails) { (key, check) => {
        val hTwos = doc.select("li.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == title)

        hTwo must not be (None)
        val section = hTwo.get.parents().select("li").first()
        check(section) must be(true)
      }
      }
    }

    "include the provided data for a NonUKIBANNumber" in new ViewFixture {

      private val title = Messages("bankdetails.bankaccount.accountname") + ": " + "Account Name"

      private val bankDetailsSet = Set(
        Messages("bankdetails.bankaccount.iban") + ": 890834561",
        Messages("bankdetails.bankaccount.accounttype.uk.lbl") + ": " + Messages("lbl.no"),
        Messages("bankdetails.bankaccount.accounttype.lbl") + ": " + Messages("bankdetails.summary.accounttype.lbl.01")
      )

      val sectionCheckstestUKBankDetails = Table[String, Element => Boolean](
        ("title key", "check"),
        (title, checkElementTextIncludes(_,
          "890834561",
          "bankdetails.bankaccount.accounttype.uk.lbl", "lbl.no",
          "bankdetails.bankaccount.accounttype.lbl", "bankdetails.summary.accounttype.lbl.01")
          ), (title, checkListContainsItems(_, bankDetailsSet))
      )

      def view = {
        val testdata = Seq(BankDetails(Some(PersonalAccount), Some(BankAccount("Account Name", NonUKIBANNumber("890834561")))))

        views.html.bankdetails.summary(testdata, true, true, true)
      }

      forAll(sectionCheckstestUKBankDetails) { (key, check) => {
        val hTwos = doc.select("li.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == title)

        hTwo must not be (None)
        val section = hTwo.get.parents().select("li").first()
        check(section) must be(true)
      }
      }
    }
  }
}