package controllers.businessactivities



import models.businessactivities.{FirstTurnoverAmls, TurnerOverExpectIn12MonthsRelatedToAMLS, BusinessActivities}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, InvolvedInOtherYes}
import models.businessmatching.{BusinessActivities => activities, BusinessMatching}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import utils.AuthorisedFixture



import scala.concurrent.Future

class TurnoverExpectIn12MonthsRelatedToAMLSControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures{

    trait Fixture extends AuthorisedFixture {
      self =>

      val controller = new TurnerOverExpectIn12MonthsRelatedToAMLSController {
        override val dataCacheConnector = mock[DataCacheConnector]
        override val authConnector = self.authConnector
      }
    }


  "TurnoverExpectIn12MonthsRelatedToAMLSController" must {

    "on get display the Turnover Expect In 12Months Related To AMLS page" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[TurnerOverExpectIn12MonthsRelatedToAMLS](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("How much turnover do you expect in the next 12 months Related To AMLS?")
    }

    "on get display the Role Within Business page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(BusinessActivities(None, Some(FirstTurnoverAmls)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      // TODO
//      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "01"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.WhatYouNeedController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "01"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.WhatYouNeedController.get().url))
    }


  }
}
