package controllers.asp

import connectors.DataCacheConnector
import models.asp.{OtherBusinessTaxMattersYes, Asp}
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


class OtherBusinessTaxMattersControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new OtherBusinessTaxMattersController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "OtherBusinessTaxMattersController" must {

    "on get display the are you registered with HMRC to handle other business's tax matters page" in new Fixture {
      when(controller.dataCacheConnector.fetch[Asp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("asp.other.business.tax.matters.title"))
    }

    "on get display the the Does your business use the services of another Trust or Company Service Provider page with pre populated data" in new Fixture {
      when(controller.dataCacheConnector.fetch[Asp](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(Asp(otherBusinessTaxMatters = Some(OtherBusinessTaxMattersYes("12345678"))))))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("12345678")
    }

    "on post with valid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "otherBusinessTaxMatters" -> "true",
        "agentRegNo" -> "12345678"
      )

      when(controller.dataCacheConnector.fetch[Asp](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Asp](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequestInvalid = request.withFormUrlEncodedBody(
        "otherBusinessTaxMatters" -> "true",
        "agentRegNo" -> "adbg123312589"
      )

      val result = controller.post()(newRequestInvalid)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include(Messages("error.invalid.length.asp.agentRegNo"))

    }

    "On post with missing boolean data" in new Fixture {

      val newRequestInvalid = request.withFormUrlEncodedBody(
        "otherBusinessTaxMatters" -> ""
      )

      val result = controller.post()(newRequestInvalid)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include(Messages("error.required.asp.other.business.tax.matters"))
    }

    "On post with missing agent registration number" in new Fixture {

      val newRequestInvalid = request.withFormUrlEncodedBody(
        "otherBusinessTaxMatters" -> "true",
        "agentRegNo" -> ""
      )

      val result = controller.post()(newRequestInvalid)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include(Messages("error.required.asp.agentRegNo"))
    }

    "on post with valid data in edit mode" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "otherBusinessTaxMatters" -> "true",
        "agentRegNo" -> "12345678"
      )

      when(controller.dataCacheConnector.fetch[Asp](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Asp](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

  }

}
