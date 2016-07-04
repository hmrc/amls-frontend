package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.aboutthebusiness._
import org.joda.time.LocalDate
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

class ActivityStartDateControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ActivityStartDateController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  // scalastyle:off
  private val startDate = ActivityStartDate(new LocalDate(2010, 2, 22))
  private val aboutTheBusiness = AboutTheBusiness(None, Some(startDate), None, None)

  val emptyCache = CacheMap("", Map.empty)

  "ActivityStartDateController" must {

    "Get Option:" must {

      "load ActivityStartDate page" in new Fixture {

        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.activity.start.date.title"))
      }

      "load ActivityStartDate with pre-populated data" in new Fixture {

        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))
        val result = controller.get()(request)
        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=startDate.day]").`val` must include("22")
        document.select("input[name=startDate.month]").`val` must include("2")

      }
    }

    "Post" must {

      "successfully redirect VATRegisteredController" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "startDate.day" -> "12",
          "startDate.month" -> "5",
          "startDate.year" -> "1999"
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))
        when (controller.dataCache.save(any(), any())(any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.VATRegisteredController.get().url))
      }

      "on post with invalid data show error" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "startDate.day" -> "",
          "startDate.month" -> "",
          "startDate.year" -> ""
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        println(contentAsString(result))
        contentAsString(result) must include(Messages("error.expected.jodadate.format"))

      }
    }
  }
}
