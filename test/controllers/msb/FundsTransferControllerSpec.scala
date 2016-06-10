package controllers.msb

import connectors.DataCacheConnector
import models.moneyservicebusiness._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class FundsTransferControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new FundsTransferController {
      val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }
  val emptyCache = CacheMap("", Map.empty)
  "FundsTransferControllerSpec" should {

    "on get, display the 'Do you transfer money without using formal banking systems?' page" in new Fixture {
      when(controller.dataCache.fetch[MoneyServiceBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.fundstransfer.heading"))
    }

    "on get, display the 'Do you transfer money without using formal banking systems?' page with pre populated data" in new Fixture {

      when(controller.dataCache.fetch[MoneyServiceBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(fundsTransfer = Some(FundsTransfer(true))))))
      val result = controller.get()(request)

      status(result) must be(OK)

      val page = Jsoup.parse(contentAsString(result))

      page.select("input[type=radio][name=transferWithoutFormalSystems][checked]").`val`() must be("true")

    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> "true"
      )

      when(controller.dataCache.fetch[MoneyServiceBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[MoneyServiceBusiness](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> ""
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include(Messages("error.required.msb.fundsTransfer"))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> "true"
      )

      when(controller.dataCache.fetch[MoneyServiceBusiness](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[MoneyServiceBusiness](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with valid data and user has selected option as no" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> "false"
      )

      when(controller.dataCache.fetch[MoneyServiceBusiness](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[MoneyServiceBusiness](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }
  }

}