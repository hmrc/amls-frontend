package controllers.businessmatching

import connectors.DataCacheConnector
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{TypeOfBusiness, BusinessMatching}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class TypeOfBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TypeOfBusinessController {
      override val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector: AuthConnector = self.authConnector
    }
  }

  "TypeOfBusinessController" must {

    val emptyCache = CacheMap("", Map.empty)

    "display business Types Page" in new Fixture {

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.title() must be (Messages("businessmatching.typeofbusiness.title"))
    }

    "display main Summary Page" in new Fixture {

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(None, None, Some(TypeOfBusiness("test"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.title() must be (Messages("businessmatching.typeofbusiness.title"))
      document.select("input[type=text]").`val`() must be("test")
    }

    "post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "typeOfBusiness" -> "text"
      )

      when(controller.dataCache.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisterServicesController.get().url))

    }

    "post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "typeOfBusiness" -> "11"*40
      )

      when(controller.dataCache.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("Maximum length is 40")

    }

    "post with missing mandatory field" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
      )

      when(controller.dataCache.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required"))
    }
  }
}
