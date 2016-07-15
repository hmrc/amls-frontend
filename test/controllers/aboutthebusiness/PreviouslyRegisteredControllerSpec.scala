package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.Country
import models.aboutthebusiness._
import models.businessactivities.BusinessActivities
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future


class PreviouslyRegisteredControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {
  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new PreviouslyRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessRegisteredWithHMRCBeforeController" must {

    "on get display the previously registered with HMRC page" in new Fixture {
      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("aboutthebusiness.registeredformlr.title"))
    }

    "on get display the previously registered with HMRC with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(AboutTheBusiness(Some(PreviouslyRegisteredYes("12345678"))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=true]").hasAttr("checked") must be(true)
    }

    "on post with valid data and businesstype is corporate" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "previouslyRegistered" -> "true",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
      when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
        .thenReturn(Some(AboutTheBusiness(Some(PreviouslyRegisteredNo))))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.VATRegisteredController.get().url))
    }

    "on post with valid data and load confirm address page when businessType is SoleProprietor" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "previouslyRegistered" -> "true",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails("BusinessName", None,
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
      when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
        .thenReturn(Some(AboutTheBusiness(Some(PreviouslyRegisteredNo))))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ConfirmRegisteredOfficeController.get().url))
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "previouslyRegistered" -> "true",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails("BusinessName", None,
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"),Country("United Kingdom", "GB")), "ghghg")

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
      when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
        .thenReturn(None)

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ConfirmRegisteredOfficeController.get().url))
    }

    "on post with valid data in edit mode and load summary page" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "previouslyRegistered" -> "true",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
      when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
        .thenReturn(Some(AboutTheBusiness(Some(PreviouslyRegisteredNo))))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "prevMLRRegNo" -> "12345678"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include(Messages("err.summary"))
    }
  }
}
