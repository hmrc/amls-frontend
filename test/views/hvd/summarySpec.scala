package views.hvd

import forms.{InvalidForm, ValidForm, Form2}
import models.hvd._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture

import scala.collection.JavaConversions._


class summarySpec extends WordSpec with MustMatchers with OneAppPerSuite with TableDrivenPropertyChecks {

  "summary view" must {
    "have correct title" in new ViewFixture {


      def view = views.html.hvd.summary(Hvd(), true)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.hvd.summary(Hvd(), true)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.hvd"))
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
      ("hvd.products.title", checkListContainsItems(_, fullProductSet))
    )

    "include the provided data" in new ViewFixture {
      def view = {
        val testdata = Hvd(
          cashPayment = Some(CashPaymentYes(LocalDate.parse("2012-6-20"))),
          products = Some(Products(Set(
            Alcohol,
            Tobacco,
            Antiques,
            Cars,
            OtherMotorVehicles,
            Caravans,
            Jewellery,
            Gold,
            ScrapMetals,
            MobilePhones,
            Clothing,
            Other("Other Product")
          )))
        )

        views.html.hvd.summary(testdata, true)
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
