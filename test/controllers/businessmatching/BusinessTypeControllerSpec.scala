package controllers.businessmatching

import connectors.DataCacheConnector
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
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

class BusinessTypeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new BusinessTypeController {
      override private[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override protected val authConnector: AuthConnector = self.authConnector
    }
  }

  "BusinessTypeController" must {

    val emptyCache = CacheMap("", Map.empty)

    "display business Types Page" in new Fixture {

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.title() must be (Messages("businessmatching.businessType.title"))
    }

    "display Registration Number page for CORPORATE_BODY" in new Fixture {

     val reviewDtls = ReviewDetails("BusinessName", Some(controller.CORPORATE_BODY),
       Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), "GB"), "ghghg", "XE0001234567890")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(None,Some(reviewDtls)))))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(controllers.routes.MainSummaryController.onPageLoad().url))
    }

    "display Registration Number page for LLP" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(controller.LLP),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), "GB"), "ghghg", "XE0001234567890")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(None,Some(reviewDtls)))))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(controllers.routes.MainSummaryController.onPageLoad().url))
    }

    "display Type of Business Page" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(controller.UNINCORPORATED_BODY),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), "GB"), "ghghg", "XE0001234567890")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(None,Some(reviewDtls)))))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(routes.TypeOfBusinessController.get().url))
    }

    "display Register Services Page" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(controller.PARTNERSHIP),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), "GB"), "ghghg", "XE0001234567890")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(None,Some(reviewDtls)))))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(routes.RegisterServicesController.get().url))
    }


    "post with updated business matching data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "businessType" -> "01"
      )

      val reviewDtls = ReviewDetails("BusinessName", Some("SOP"),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), "GB"), "ghghg", "XE0001234567890")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(None,Some(reviewDtls)))))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.MainSummaryController.onPageLoad().url))

    }

    "post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "businessType" -> "01"
      )

      when(controller.dataCache.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.MainSummaryController.onPageLoad().url))

    }

    "post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "businessType" -> "11"
      )

      when(controller.dataCache.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("err.summary"))

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
