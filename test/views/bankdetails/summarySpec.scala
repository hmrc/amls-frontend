package views.bankdetails

import models.bankdetails._
import org.jsoup.nodes.Element
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.MustMatchers
import org.scalatest.prop.PropertyChecks
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

import scala.collection.JavaConversions._


class summarySpec extends GenericTestHelper with MustMatchers with PropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

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
    parent.select("li").toSet.map((el: Element) => el.text()).tail must be(keysToFind.map(k => Messages(k)))
    true
  }

  def checkElementTextIncludes(el: Element, keys: String*) = {
    keys.foreach { k =>
      el.text() must include(Messages(k))
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

      private val sectionCheckstestUKBankDetails = Table[String, Element => Boolean](
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

      forAll(sectionCheckstestUKBankDetails) { (_, check) => {
        val hTwos = doc.select("li.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == title)

        hTwo must not be None
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

    "hide the first six numbers of a UKAccount number" when {
      "complete" when {
        "user is making an amendment" in {

          val accountNameGen: Gen[String] = Gen.alphaStr //map (_.take(8))

          val genUKAccount: Gen[UKAccount] = for {
            accountNumber <- Gen.numStr suchThat(_.length == 8)
            accountName <- accountNameGen
            sortCode <- Gen.numStr suchThat(_.length == 6)
          } yield {
            UKAccount(accountNumber, sortCode)
          }

          implicit val arbB: Arbitrary[UKAccount] = Arbitrary {
            genUKAccount
          }

          forAll{ (aName: String, uk: UKAccount) =>
            whenever(
              aName.length > 0 && uk.sortCode.length > 0
            ) {
              new ViewFixture {

                println(">>>>>>" + BankAccount(aName, uk))

                val ukBankAccount = BankAccount(aName, uk)
                val testdata = Seq(BankDetails(Some(PersonalAccount), Some(BankAccount(aName, uk))))

                def view = views.html.bankdetails.summary(testdata, true, true, true)

                val section = doc.select("li.check-your-answers ul").first().select("li").eq(1).first()

                section.text() must be("")

              }
            }
          }

        }
        "user is making a variation" in {

        }
        "user is making a renewal" in {

        }
      }
    }

    "hide the first six numbers of a NonUKAccountNumber" when {
      "complete" in {

      }
    }

    "hide the first six numbers of a NonUKIBANNumber" when {
      "complete" in {

      }
    }

  }
}