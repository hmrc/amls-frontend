package views.aboutthebusiness

import models.aboutthebusiness._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import views.{HtmlAssertions, ViewFixture}

import scala.collection.JavaConversions._


class summarySpec extends WordSpec
  with MustMatchers
  with OneAppPerSuite
  with HtmlAssertions
  with TableDrivenPropertyChecks {

  "summary view" must {
    "have correct title" in new ViewFixture {


      def view = views.html.aboutthebusiness.summary(AboutTheBusiness())

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.aboutthebusiness.summary(AboutTheBusiness())

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.aboutbusiness"))
    }

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      ("aboutthebusiness.registeredformlr.title",checkElementTextIncludes(_, "aboutthebusiness.registeredformlr.mlrregno.lbl")),
      ("aboutthebusiness.activity.start.date.title",checkElementTextIncludes(_, "lbl.start.date")),
      ("aboutthebusiness.registeredforvat.title",checkElementTextIncludes(_, "lbl.vat.reg.number")),
      ("aboutthebusiness.registeredforcorporationtax.title",checkElementTextIncludes(_, "aboutthebusiness.registeredforcorporationtax.taxReference")),
      ("aboutthebusiness.registeredoffice.title",checkElementTextIncludes(_, "line1","line2","line3","line4","AB12CD")),
      ("aboutthebusiness.contactingyou.title",checkElementTextIncludes(_, "aboutthebusiness.contactingyou.phone.lbl","aboutthebusiness.contactingyou.email.lbl")),
      ("aboutthebusiness.correspondenceaddress.postal.address",
        checkElementTextIncludes(_, "your name", "business name","line1","line2","line3","line4","AB12CD"))
    )

    "include the provided data" in new ViewFixture {

      def view = views.html.aboutthebusiness.summary(
        AboutTheBusiness(
          Some(PreviouslyRegisteredYes("1234")),
          Some(ActivityStartDate(new LocalDate(2016, 1, 2))),
          Some(VATRegisteredYes("2345")),
          Some(CorporationTaxRegisteredYes("3456")),
          Some(ContactingYou("01234567890", "test@test.com")),
          Some(RegisteredOfficeUK("line1","line2",Some("line3"),Some("line4"),"AB12CD")),
          Some(UKCorrespondenceAddress("your name", "business name","line1","line2",Some("line3"),Some("line4"),"AB12CD")),
          false
        )
      )

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")

        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be (None)
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }
      }
    }
  }
}
