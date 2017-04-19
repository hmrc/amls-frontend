package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.businessactivities.{TransactionRecordNo, TransactionRecord}
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class customer_transaction_recordsSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "customer_transaction_records view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[TransactionRecord] = Form2(TransactionRecordNo)

      def view = views.html.businessactivities.customer_transaction_records(form2, true)

      doc.title must startWith(Messages("businessactivities.keep.customer.records.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[TransactionRecord] = Form2(TransactionRecordNo)

      def view = views.html.businessactivities.customer_transaction_records(form2, true)

      heading.html must be(Messages("businessactivities.keep.customer.records.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isRecorded") -> Seq(ValidationError("not a message Key")),
          (Path \ "transactions") -> Seq(ValidationError("second not a message Key")),
          (Path \ "name") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = views.html.businessactivities.customer_transaction_records(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")

      doc.getElementById("isRecorded")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("transactions")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

      doc.getElementById("name").parent
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

    }
  }
}