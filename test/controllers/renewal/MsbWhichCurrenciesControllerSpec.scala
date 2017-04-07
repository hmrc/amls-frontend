package controllers.renewal

import cats.implicits._
import models.moneyservicebusiness.{BankMoneySource, WholesalerMoneySource}
import models.renewal.{MsbWhichCurrencies, Renewal}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class MsbWhichCurrenciesControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val renewalService = mock[RenewalService]
    val request = addToken(authRequest)

    lazy val controller = new MsbWhichCurrenciesController(self.authConnector, renewalService)

    when {
      renewalService.getRenewal(any(), any(), any())
    } thenReturn Future.successful(Renewal().some)
  }

  trait FormSubmissionFixture extends Fixture {
    val validFormRequest = request.withFormUrlEncodedBody(
      "currencies[0]" -> "USD",
      "currencies[1]" -> "GBP",
      "currencies[2]" -> "BOB",
      "bankMoneySource" -> "Yes",
      "bankNames" -> "Bank names",
      "wholesalerMoneySource" -> "Yes",
      "wholesalerNames" -> "wholesaler names",
      "customerMoneySource" -> "Yes",
      "usesForeignCurrencies" -> "Yes"
    )

    when {
      renewalService.updateRenewal(any())(any(), any(), any())
    } thenReturn Future.successful(mock[CacheMap])
  }

  "Calling the GET action" must {
    "return the correct view" when {
      "edit is false" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select(".heading-xlarge").text mustBe Messages("renewal.msb.whichcurrencies.header")
      }

      "edit is true" in new Fixture {
        val result = controller.get(true)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select("form").first.attr("action") mustBe routes.MsbWhichCurrenciesController.post(true).url
      }

      "reads the current value from the renewals model" in new Fixture {

        when {
          renewalService.getRenewal(any(), any(), any())
        } thenReturn Future.successful(Renewal(msbWhichCurrencies = MsbWhichCurrencies(Seq("EUR"), None, None, None, None).some).some)

        val result = controller.get(true)(request)
        val doc = Jsoup.parse(contentAsString(result))

        doc.select("select[name=currencies[0]] option[selected]").attr("value") mustBe "EUR"

        verify(renewalService).getRenewal(any(), any(), any())
      }
    }
  }

  "Calling the POST action" when {
    "posting valid data" must {
      "redirect to the next page in the flow" in new FormSubmissionFixture {
        val result = controller.post()(validFormRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe controllers.renewal.routes.PercentageOfCashPaymentOver15000Controller.get().url.some
      }

      "redirect to the summary page when edit = true" in new FormSubmissionFixture {
        val result = controller.post(edit = true)(validFormRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe controllers.renewal.routes.SummaryController.get().url.some
      }

      "return a bad request" when {
        "the form fails validation" in new FormSubmissionFixture {
          val result = controller.post()(request)

          status(result) mustBe BAD_REQUEST
          verify(renewalService, never()).updateRenewal(any())(any(), any(), any())
        }
      }

      "save the model data into the renewal object" in new FormSubmissionFixture {
        val result = await(controller.post()(validFormRequest))
        val captor = ArgumentCaptor.forClass(classOf[Renewal])

        verify(renewalService).updateRenewal(captor.capture())(any(), any(), any())

        captor.getValue.msbWhichCurrencies mustBe Some(MsbWhichCurrencies(
          Seq("USD", "GBP", "BOB"),
          Some(true),
          Some(BankMoneySource("Bank names")),
          Some(WholesalerMoneySource("wholesaler names")),
          Some(true)
        ))
      }
    }
  }
}
