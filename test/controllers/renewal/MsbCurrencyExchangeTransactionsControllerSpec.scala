package controllers.renewal

import connectors.DataCacheConnector
import models.moneyservicebusiness._
import models.renewal.{CETransactions, Renewal}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class MsbCurrencyExchangeTransactionsControllerSpec extends GenericTestHelper with MockitoSugar  {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new MsbCurrencyExchangeTransactionsController (
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "MsbCurrencyExchangeTransactionsController" must {

    "load the page 'How many currency exchange transactions'" in new Fixture {

      when(controller.dataCacheConnector.fetch[Renewal](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("renewals.msb.ce.transactions.expected.title"))
    }

    "load the page 'How many currency exchange transactions' with pre populated data" in new Fixture  {

      when(controller.dataCacheConnector.fetch[Renewal](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Renewal(
        ceTransactions = Some(CETransactions("12345678963"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("12345678963")

    }

    "Show error message when user has not filled the mandatory fields" in new Fixture  {

      val newRequest = request.withFormUrlEncodedBody(
        "ceTransaction" -> ""
      )

      when(controller.dataCacheConnector.fetch[Renewal](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Renewal](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include (Messages("error.required.renewal.ce.transactions.in.12months"))

    }

    "Successfully save data in save4later and navigate to Next page" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody (
        "ceTransaction" -> "12345678963"
      )

      when(controller.dataCacheConnector.fetch[Renewal](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Renewal](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.renewal.routes.MsbWhichCurrenciesController.get().url))
    }

    "Successfully save data in save4later and navigate to Summary page in edit mode" in new Fixture {

      val incomingModel = Renewal(
      )

      val outgoingModel = incomingModel.copy(
        ceTransactions = Some(
          CETransactions("12345678963")
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody (
        "ceTransaction" -> "12345678963"
      )

      when(controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))
        (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(controller.dataCacheConnector.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
    }

  }
}
