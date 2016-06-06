package controllers.msb

import connectors.DataCacheConnector
import models.moneyservicebusiness.{BusinessAppliedForPSRNumberYes, MoneyServiceBusiness}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BusinessAppliedForPSRNumberControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new BusinessAppliedForPSRNumberController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessAppliedForPSRNumberController" must {

    "on get display the page 'business applied for a Payment Systems Regulator (PSR) registration number?'" in new Fixture {
      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.psr.number.title"))
    }


    "on get display the page 'business applied for a Payment Systems Regulator (PSR) registration number?' with pre populated data" in new Fixture {
      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(
        Some(MoneyServiceBusiness(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123456"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=true]").hasAttr("checked") must be(true)
      document.select("input[name=regNumber]").`val` must be("123456")
    }

    "on post with valid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "appliedFor" -> "true",
        "regNumber" -> "123789"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "appliedFor" -> "true",
        "regNumber" -> ""
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document  = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include(Messages("error.invalid.msb.psr.number"))
    }

    "on post with valid data in edit mode" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "appliedFor" -> "true",
        "regNumber" -> "123789"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }
  }
}
