package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.Country
import models.aboutthebusiness._
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import org.mockito.Matchers.{eq => meq, _}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class ActivityStartDateControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

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

      "successfully redirect to ConfirmRegisteredOfficeController if not org or partnership" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "startDate.day" -> "12",
          "startDate.month" -> "5",
          "startDate.year" -> "1999"
        )

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
        when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
          .thenReturn(Some(AboutTheBusiness(Some(PreviouslyRegisteredNo))))

        when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when (controller.dataCache.save(any(), any())(any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ConfirmRegisteredOfficeController.get().url))
      }

      "successfully redirect to VATRegisteredController org or partnership" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "startDate.day" -> "12",
          "startDate.month" -> "5",
          "startDate.year" -> "1999"
        )

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

        when (controller.dataCache.save(any(), any())(any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
        when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
          .thenReturn(Some(AboutTheBusiness(Some(PreviouslyRegisteredNo))))

        when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.VATRegisteredController.get().url))
      }

      "show error with invalid" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "startDate.day" -> "",
          "startDate.month" -> "",
          "startDate.year" -> ""
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.expected.jodadate.format"))

      }

      "show error with year field too short" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "startDate.day" -> "1",
          "startDate.month" -> "3",
          "startDate.year" -> "16"
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.expected.jodadate.format"))
      }

      "show error with year field too long" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "startDate.day" -> "1",
          "startDate.month" -> "3",
          "startDate.year" -> "19782"
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.expected.jodadate.format"))
      }
    }
  }
}
