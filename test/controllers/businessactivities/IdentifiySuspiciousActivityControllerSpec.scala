package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities.{IdentifySuspiciousActivity, BusinessActivities, BusinessFranchiseYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import utils.AuthorisedFixture

import scala.concurrent.Future

class IdentifiySuspiciousActivityControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new IdentifySuspiciousActivityController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "IdentifySuspiciousActivityController" must {

    "use correct services" in new Fixture {
      BusinessFranchiseController.dataCacheConnector must be(DataCacheConnector)
    }

    "on get, display the Identify suspicious activity page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessactivities.identify-suspicious-activity.title"))
    }

    "on get, display the identify suspicious activity page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(BusinessActivities(identifySuspiciousActivity = Some(IdentifySuspiciousActivity(true))))))
      val result = controller.get()(request)

      status(result) must be(OK)

      val page = Jsoup.parse(contentAsString(result))

      page.select("input[type=radio][name=hasWrittenGuidance][value=true][checked]").size must be (1)
      page.select("input[type=radio][name=hasWrittenGuidance][value=false]").size must be (1)
      page.select("input[type=radio][name=hasWrittenGuidance][value=false][checked]").size must be (0)

    }

    // ignored until the National crime agency page is ready to redirect to
    "on post with valid data" ignore new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "hasWrittenGuidance" -> "true"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TransactionRecordController.get().url))
    }

    "on post with invalid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "hasWrittenGuidance" -> "grrrrr"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document  = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include("Invalid value")
    }

    //ignored until summary page is available to redirect to
    "on post with valid data in edit mode" ignore new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "hasWrittenGuidance" -> "true"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }
  }
}
