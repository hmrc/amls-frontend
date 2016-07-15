package controllers.hvd

import connectors.DataCacheConnector
import models.hvd.{Tobacco, Products, Alcohol, Hvd}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class ProductsControllerSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ProductsController {

      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "ProductsController" must {

    "load the 'What will your business sell?' page" in new Fixture  {

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("hvd.products.title"))

    }

    "pre-populate the 'What will your business sell?' page" in new Fixture  {

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Hvd(products = Some(Products(Set(Alcohol, Tobacco)))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
      document.select("input[value=02]").hasAttr("checked") must be(true)

    }

    "Successfully post the data when the option alcohol is selected" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "products[0]" -> "01",
        "products[1]" -> "02"
      )

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ExciseGoodsController.get().url))
    }

    "successfully navigate to next page when the option other than alcohol and tobacco selected " in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "products[0]" -> "03",
        "products[1]" -> "04"
      )

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.HowWillYouSellGoodsController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "products[0]" -> "01",
        "products[1]" -> "02",
        "products[2]" -> "12",
        "otherDetails" -> "test"
      )

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ExciseGoodsController.get(true).url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "products[0]" -> "01",
        "products[1]" -> "12",
        "otherDetails" -> ""
      )

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#otherDetails]").html() must include(Messages("error.required.hvd.business.sell.other.details"))
    }

    "on post with invalid data1" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "products[0]" -> "01",
        "products[1]" -> "02",
        "products[2]" -> "12",
        "otherDetails" -> "g"*256
      )

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#otherDetails]").html() must include(Messages("error.invalid.hvd.business.sell.other.details"))
    }
  }

}
