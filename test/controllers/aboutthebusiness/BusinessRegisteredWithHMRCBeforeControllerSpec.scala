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


class BusinessRegisteredWithHMRCBeforeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {
  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new PreviouslyRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "BusinessRegisteredWithHMRCBeforeController" must {

    "on get display the previously registered with HMRC page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("Has this business been registered with HMRC before?")
    }
  }

  "on get display the previously registered with HMRC with pre populated data" in new Fixture {

    when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(AboutTheBusiness(Some(PreviouslyRegisteredYes("12345678"))))))

    val result = controller.get()(request)
    status(result) must be(OK)

    val document = Jsoup.parse(contentAsString(result))
    // TODO
    //      document.select("input[value=01]").hasAttr("checked") must be(true)
  }

  "on post with valid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "previouslyRegistered" -> "true",
      "prevMLRRegNo" -> "12345678"
    )

    when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

    when(controller.dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(None))

    val result = controller.post()(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.VATRegisteredController.get().url))
  }

  "on post with invalid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "prevMLRRegNo" -> "12345678"
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
      "previouslyRegisteredYes" -> "true"
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
