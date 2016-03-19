package controllers.businessmatching

import connectors.DataCacheConnector
import models.businessmatching.{CompanyRegistrationNumber, BusinessMatching}
import org.jsoup.Jsoup
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


class CompanyRegistrationNumberControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new CompanyRegistrationNumberController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val businessMatching = BusinessMatching(companyRegistrationNumber = Some(CompanyRegistrationNumber("12345678")))

  "CompanyRegistrationNumberController" must {

    "on get() display company registration number page" in new Fixture {
      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.title() must be (Messages("businessmatching.registrationnumber.title"))
    }

    "on get() display existing data if it exists" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching)))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=companyRegistrationNumber]").`val` must be("12345678")
    }

    "on post() give a bad request if invalid data sent" in new Fixture {

        val invalidRequest = request.withFormUrlEncodedBody(
          "companyRegistrationNumber" -> "INVALID_DATA"
        )

        val result = controller.post()(invalidRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title() must be (Messages("businessmatching.registrationnumber.title"))
    }

    "on post() redirect correctly if valid data sent and edit is true" in new Fixture {

      val validRequest = request.withFormUrlEncodedBody(
        "companyRegistrationNumber" -> "12345678"
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching)))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(validRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisterServicesController.get().url))
    }

    "on post() redirect correctly if valid data sent and edit is false" in new Fixture {

      val validRequest = request.withFormUrlEncodedBody(
        "companyRegistrationNumber" -> "12345678"
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching)))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(false)(validRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.MainSummaryController.onPageLoad().url))
    }

  }

}
