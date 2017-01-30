package controllers.hvd

import connectors.DataCacheConnector
import models.hvd.Hvd
import models.status.{SubmissionDecisionApproved, NotCompleted}
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import services.StatusService
import utils.AuthorisedFixture
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class ReceiveCashPaymentsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ReceiveCashPaymentsController {
      override val cacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService: StatusService = mock[StatusService]
    }

    when(controller.cacheConnector.fetch[Hvd](eqTo(Hvd.key))(any(), any(), any()))
      .thenReturn(Future.successful(None))

    when(controller.cacheConnector.save[Hvd](eqTo(Hvd.key), any())(any(), any(), any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))
  }

  "ReceiveCashPaymentsController" must {

    "load the page" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      val result = controller.get()(request)
      status(result) mustEqual OK
    }

    "redirect to Page not found" when {
      "application is in variation mode" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }
    "show a bad request with an invalid request" in new Fixture {

      val result = controller.post()(request)
      status(result) mustEqual BAD_REQUEST
    }

    "redirect to summary on successful edit" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "receivePayments" -> "false"
      )

      val result = controller.post(true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "redirect to next page on successful submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "receivePayments" -> "false"
      )

      val result = controller.post(false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.PercentageOfCashPaymentOver15000Controller.get().url)
    }
  }
}
