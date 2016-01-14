package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.aboutthebusiness._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future


class BusinessRegisteredForVATControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {
  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new BusinessRegisteredForVATController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "BusinessRegisteredForVATController" must {

    "on get display the registered for VAT page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("Is this business registered for VAT?")
    }
  }

  "on get display the registered for VAT page with pre populated data" in new Fixture {

    when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(AboutTheBusiness(Some(PreviouslyRegisteredYes("")), Some(RegisteredForVATYes("123456789"))))))

    val result = controller.get()(request)
    status(result) must be(OK)

    val document = Jsoup.parse(contentAsString(result))
    // TODO
    //      document.select("input[value=01]").hasAttr("checked") must be(true)
  }

  "on post with valid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "registeredForVAT" -> "true",
      "registeredForVATYes" -> "123456789"
    )

    when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

    when(controller.dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(None))

    val result = controller.post()(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.BusinessRegisteredForVATController.get().url))
  }

  "on post with invalid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "registeredForVATYes" -> "1234567890"
    )

    val result = controller.post()(newRequest)
    status(result) must be(BAD_REQUEST)

    val document = Jsoup.parse(contentAsString(result))
    // TODO
    //      document.select("input[name=other]").`val` must be("foo")
  }

  // to be valid after summary edit page is ready
  /* "on post with valid data in edit mode" in new Fixture {

     val newRequest = request.withFormUrlEncodedBody(
       "registeredForVATYes" -> "true"
     )

     when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

     when(controller.dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(None))

     val result = controller.post(true)(newRequest)
     status(result) must be(SEE_OTHER)
     redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.SummaryController.get().url))
   }*/



}


