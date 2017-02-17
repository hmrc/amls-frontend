package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.aboutthebusiness.{AboutTheBusiness, UKCorrespondenceAddress}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import scala.collection.JavaConversions._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class CorrespondenceAddressControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new CorrespondenceAddressController {
      override val dataConnector: DataCacheConnector = mock[DataCacheConnector]

      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "CorrespondenceAddressController" should {

    "respond to a get request correctly with different form values" when {

      "data exists in the keystore" in new Fixture {

        val correspondenceAddress = UKCorrespondenceAddress("Name Test", "Test", "Test", "Test", Some("test"), None, "Test")
        val aboutTheBusiness = AboutTheBusiness(None, None, None, None, None,None, Some(correspondenceAddress))
        val fetchResult = Future.successful(Some(aboutTheBusiness))

        when(controller.dataConnector.fetch[AboutTheBusiness](any())(any(), any(), any())).thenReturn(fetchResult)

        val result = controller.get(false)(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title must include(Messages("aboutthebusiness.correspondenceaddress.title"))
        Jsoup.parse(contentAsString(result)).select("#yourName").`val` must include("Name Test")

      }

      "no data exists in the keystore" in new Fixture {

        val fetchResult = Future.successful(None)
        when(controller.dataConnector.fetch[AboutTheBusiness](any())(any(), any(), any())).thenReturn(fetchResult)

        val result = controller.get(false)(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title must include(Messages("aboutthebusiness.correspondenceaddress.title"))
        Jsoup.parse(contentAsString(result)).select("#isUK-true").attr("checked") mustBe "checked"

      }
    }

    "respond to a post request correctly" when {

      val emptyCache = CacheMap("", Map.empty)

      "a valid form request is sent in the body" in new Fixture {

        val fetchResult = Future.successful(None)

        val newRequest = request.withFormUrlEncodedBody(
          "yourName" -> "Name",
          "businessName" -> "Business Name",
          "isUK"         -> "true",
          "addressLine1" -> "Add Line 1",
          "addressLine2" -> "Add Line 2",
          "addressLine3" -> "",
          "addressLine4" -> "",
          "postCode" -> "AA1 1AA"
        )

        when(controller.dataConnector.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))

      }

      "fail submission on invalid address" in new Fixture {

        val fetchResult = Future.successful(None)

        val newRequest = request.withFormUrlEncodedBody(
          "yourName" -> "Name",
          "businessName" -> "Business Name",
          "isUK"         -> "true",
          "addressLine1" -> "Add Line 1 & 3",
          "addressLine2" -> "Add Line 2 *",
          "addressLine3" -> "$$$",
          "addressLine4" -> "##",
          "postCode" -> "AA1 1AA"
        )

        when(controller.dataConnector.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(BAD_REQUEST)

        val document: Document  = Jsoup.parse(contentAsString(result))
        val errorCount = 4
        val elementsWithError : Elements = document.getElementsByClass("error-notification")
        elementsWithError.size() must be(errorCount)
        for (ele: Element <- elementsWithError) {
          ele.html() must include(Messages("err.text.validation"))
        }
      }

      "an invalid form request is sent in the body" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "yourName" -> "Name",
          "businessName" -> "Business Name",
          "invalid" -> "AA1 1AA"
        )

        val result = controller.post(false)(newRequest)
        status(result) must be(BAD_REQUEST)

      }
    }
  }

}
