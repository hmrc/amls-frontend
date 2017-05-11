package controllers.renewal

import connectors.DataCacheConnector
import models.renewal.{PaymentMethods, ReceiveCashPayments, Renewal}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class ReceiveCashPaymentsControllerSpec extends GenericTestHelper with MockitoSugar {

  lazy val mockDataCacheConnector = mock[DataCacheConnector]
  lazy val mockRenewalService = mock[RenewalService]

  val receiveCashPayments = ReceiveCashPayments(
    Some(PaymentMethods(true, true,Some("other"))
  ))

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ReceiveCashPaymentsController (
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )

    when(mockRenewalService.getRenewal(any(),any(),any()))
      .thenReturn(Future.successful(None))

    when(mockRenewalService.updateRenewal(any())(any(),any(),any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))
  }

  "ReceiveCashPaymentsController" when {

    "get is called" must {
      "load the page" when {
        "renewal data is found for receiving payments" in new Fixture {

          when(mockRenewalService.getRenewal(any(),any(),any()))
            .thenReturn(Future.successful(Some(Renewal(receiveCashPayments = Some(receiveCashPayments)))))

          val result = controller.get()(request)
          status(result) mustEqual OK
        }
        "no renewal data is found" in new Fixture {
          val result = controller.get()(request)
          status(result) mustEqual OK
        }
      }
    }

    "post is called" must {
      "show a bad request with an invalid request" in new Fixture {

        val result = controller.post()(request)
        status(result) mustEqual BAD_REQUEST
      }

      "redirect to summary on successful submission" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "receivePayments" -> "false"
        )

        val result = controller.post(true)(newRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
      }

    }

  }
}
