package controllers.tcsp

import connectors.DataCacheConnector
import models.tcsp.{ServicesOfAnotherTCSPYes, Tcsp}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ServicesOfAnotherTCSPControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ServicesOfAnotherTCSPController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "ServicesOfAnotherTCSPController" must {

    "on get display the Does your business use the services of another Trust or Company Service Provider page" in new Fixture {
      when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "on get display the the Does your business use the services of another Trust or Company Service Provider page with pre populated data" in new Fixture {
      when(controller.dataCacheConnector.fetch[Tcsp](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(Tcsp(servicesOfAnotherTCSP = Some(ServicesOfAnotherTCSPYes("12345678"))))))
      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "on post with valid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "servicesOfAnotherTCSP" -> "true",
        "mlrRefNumber" -> "12345678"
      )

      when(controller.dataCacheConnector.fetch[Tcsp](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Tcsp](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequestInvalid = request.withFormUrlEncodedBody(
        "servicesOfAnotherTCSP" -> "true",
        "mlrRefNumber" -> "adbg1233"
      )

      val result = controller.post()(newRequestInvalid)
      status(result) must be(BAD_REQUEST)
    }

    "On post with missing boolean data" in new Fixture {

      val newRequestInvalid = request.withFormUrlEncodedBody(
        "servicesOfAnotherTCSP" -> ""
      )

      val result = controller.post()(newRequestInvalid)
      status(result) must be(BAD_REQUEST)
    }

    "On post with missing mlr reference number" in new Fixture {

      val newRequestInvalid = request.withFormUrlEncodedBody(
        "servicesOfAnotherTCSP" -> "true",
        "mlrRefNumber" -> ""
      )

      val result = controller.post()(newRequestInvalid)
      status(result) must be(BAD_REQUEST)
    }

    "on post with valid data in edit mode" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "servicesOfAnotherTCSP" -> "true",
        "mlrRefNumber" -> "12345678"
      )

      when(controller.dataCacheConnector.fetch[Tcsp](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Tcsp](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }


  }

}
