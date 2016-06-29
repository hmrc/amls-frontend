package controllers.businessactivities

import connectors.DataCacheConnector
import models.businessactivities.{NCARegistered, BusinessActivities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future


class NCARegisteredControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new NCARegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "NCARegisteredController" must {

    "Get Option:" must {

      "load the NCA Registered page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("businessactivities.ncaRegistered.title")
      }

      "load Yes when ncaRegistered from save4later returns True" in new Fixture {

        val ncaRegistered = Some(NCARegistered(true))
        val activities = BusinessActivities(ncaRegistered = ncaRegistered)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("ncaRegistered-true").attr("checked") mustBe "checked"

      }

      "load No when ncaRegistered from save4later returns No" in new Fixture {

        val ncaRegistered = Some(NCARegistered(false))
        val activities = BusinessActivities(ncaRegistered = ncaRegistered)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("ncaRegistered-false").attr("checked") mustBe "checked"

      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes' when edit mode is on" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("ncaRegistered" -> "true")

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
      }

      "successfully redirect to the page on selection of 'Yes' when edit mode is off" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("ncaRegistered" -> "true")

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.RiskAssessmentController.get().url))
      }

    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is on" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "ncaRegistered" -> "false"
      )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is off" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "ncaRegistered" -> "false"
      )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.RiskAssessmentController.get().url))
    }


    "on post invalid data show error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()
      when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.ba.select.nca"))

    }

    "on post with invalid data show error" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "ncaRegistered" -> ""
      )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("err.summary"))

    }
  }
}
