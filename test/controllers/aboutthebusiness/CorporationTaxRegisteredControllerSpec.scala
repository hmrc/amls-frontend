package controllers.aboutthebusiness

import connectors.{BusinessMatchingConnector, BusinessMatchingReviewDetails, DataCacheConnector}
import models.aboutthebusiness.{AboutTheBusiness, CorporationTaxRegisteredYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class CorporationTaxRegisteredControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new CorporationTaxRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val businessMatchingConnector = mock[BusinessMatchingConnector]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "CorporationTaxRegisteredController" must {

    "on get display the registered for corporation tax page" in new Fixture {

      when(controller.businessMatchingConnector.getReviewDetails(any())) thenReturn Future.successful(None)

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("aboutthebusiness.registeredforcorporationtax.title"))
    }

    "on get display the registered for corporation tax page with pre populated data" in new Fixture {

      val data = AboutTheBusiness(corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1234567890")))

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(data)))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("registeredForCorporationTax-true").hasAttr("checked") must be(true)
      document.getElementById("corporationTaxReference").`val` must be("1234567890")
    }

    "on get, retrieve the corporation tax reference from business customer api if no previous entry" in new Fixture {

      val reviewDetailsModel = mock[BusinessMatchingReviewDetails]
      when(reviewDetailsModel.utr) thenReturn Some("0987654321")

      when(controller.businessMatchingConnector.getReviewDetails(any())) thenReturn Future.successful(Some(reviewDetailsModel))

      val data = AboutTheBusiness(corporationTaxRegistered = None)

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(data)))

      val result = controller.get()(request)

      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("registeredForCorporationTax-true").hasAttr("checked") must be(true)
      document.getElementById("corporationTaxReference").`val` must be("0987654321")
    }

    "on post with valid data and edit false continue to registered office page" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "registeredForCorporationTax" -> "true",
        "corporationTaxReference" -> "1234567890"
      )

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[AboutTheBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ConfirmRegisteredOfficeController.get().url))
    }

    "on post with valid data and edit true redirect to summary page" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "registeredForCorporationTax" -> "true",
        "corporationTaxReference" -> "1234567890"
      )

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[AboutTheBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.SummaryController.get().url))
    }

    "on post with no yes/no value selected show an error message" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "registeredForCorporationTax" -> ""
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#registeredForCorporationTax]").html() must include(Messages("error.required.atb.corporation.tax"))
    }

    "on post with yes with missing tax number show an error message" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "registeredForCorporationTax" -> "true",
        "corporationTaxReference" -> ""
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#corporationTaxReference]").html() must include(Messages("error.required.atb.corporation.tax.number"))
    }

    "on post with yes with invalid tax number show an error message" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "registeredForCorporationTax" -> "true",
        "corporationTaxReference" -> "ABCDEF"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#corporationTaxReference]").html() must include(Messages("error.invalid.atb.corporation.tax.number"))
    }

  }

}
