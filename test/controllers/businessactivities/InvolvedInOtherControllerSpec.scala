package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, InvolvedInOtherYes}
import models.businessmatching.{BusinessMatching, HighValueDealing, BusinessActivity, BusinessActivities => activities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture
import scala.concurrent.Future

class InvolvedInOtherControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self =>

    object InvolvedInOtherController extends InvolvedInOtherController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "InvolvedInOtherController" must {

    "on get display the is your involved in other page" in new Fixture {
      when(InvolvedInOtherController.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
      val result = InvolvedInOtherController.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessactivities.confirm-activities.title"))
    }


   /* "on get display the involved in other with pre populated data" in new Fixture {


      when(InvolvedInOtherController.dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      when(InvolvedInOtherController.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(BusinessActivities(Some(InvolvedInOtherYes("test"))))))
      val result = InvolvedInOtherController.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include ("test")

    }*/

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "true",
        "details" -> "test"
      )

      when(InvolvedInOtherController.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      when(InvolvedInOtherController.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      val result = InvolvedInOtherController.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhatYouNeedController.get().url))
    }


    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "test"
      )

      val result = InvolvedInOtherController.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      // TODO
    }


    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "true",
        "details" -> "test"
      )

      when(InvolvedInOtherController.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      when(InvolvedInOtherController.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      val result = InvolvedInOtherController.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhatYouNeedController.get().url))
    }

  }

}
