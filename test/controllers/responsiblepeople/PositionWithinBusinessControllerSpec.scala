package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.Country
import models.businessactivities.BusinessActivities
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessType, BusinessMatching}
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class PositionWithinBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new PositionWithinBusinessController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val RecordId = 1

  "PositionWithinBusinessController" must {

    "on get()" must {

      "display position within the business page" in new Fixture {
        val mockCacheMap = mock[CacheMap]
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(None))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=02]").isEmpty must be(true)
        document.select("input[value=03]").isEmpty must be(true)
        document.select("input[value=04]").isEmpty must be(false)
        document.select("input[value=05]").isEmpty must be(true)
        document.select("input[value=06]").isEmpty must be(false)

      }

      "display position within the business page when business Type is SoleProprietor" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=06]").hasAttr("checked") must be(false)
      }

      "display position within the business page when business Type is Partnership" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.Partnership),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=05]").hasAttr("checked") must be(false)
      }

      "display position within the business page when business Type is UnincorporatedBody" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.UnincorporatedBody),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=01]").hasAttr("checked") must be(false)
        document.select("input[value=02]").hasAttr("checked") must be(false)
        document.select("input[value=04]").hasAttr("checked") must be(false)
      }

      "display position within the business page when business Type is LPrLLP" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LPrLLP),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=01]").hasAttr("checked") must be(false)
        document.select("input[value=02]").hasAttr("checked") must be(false)
        document.select("input[value=04]").hasAttr("checked") must be(false)
      }

      "Prepopulate form with a single saved data" in new Fixture {

        val positions = Positions(Set(BeneficialOwner))
        val responsiblePeople = ResponsiblePeople(positions = Some(positions))
        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(responsiblePeople)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=01]").hasAttr("checked") must be(true)
        document.select("input[value=02]").hasAttr("checked") must be(false)
        document.select("input[value=03]").hasAttr("checked") must be(false)
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=05]").hasAttr("checked") must be(false)
        document.select("input[value=06]").hasAttr("checked") must be(false)
      }

      "Prepopulate form with multiple saved data" in new Fixture {

        val positions = Positions(Set(Director))
        val responsiblePeople = ResponsiblePeople(positions = Some(positions))

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(responsiblePeople)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=01]").hasAttr("checked") must be(false)
        document.select("input[value=02]").hasAttr("checked") must be(true)
        document.select("input[value=04]").hasAttr("checked") must be(false)
      }
    }

    "submit with valid data as a partnership" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positions" -> "05")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(RecordId).url))
    }

    "submit with valid data as a sole proprietor" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positions" -> "06")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(RecordId).url))
    }

    "submit with all other valid data types" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      for (i <- 1 to 4) {
        val newRequest = request.withFormUrlEncodedBody("positions" -> s"0$i")
        val result = controller.post(RecordId)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ExperienceTrainingController.get(RecordId).url))
      }
    }

    "submit with mixture of data types selected" in new Fixture {

      val positions = Positions(Set(Director))
      val responsiblePeople = ResponsiblePeople(positions = Some(positions))

      val newRequest = request.withFormUrlEncodedBody(
        "positions" -> "06",
        "positions" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(RecordId).url))

    }

    "fail submission on empty string" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positionWithinBusiness" -> "")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#positions]").html() must include(Messages("error.required.positionWithinBusiness"))
    }

    "fail submission on invalid string" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positionWithinBusiness" -> "10")
      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")
      val businessMatching = BusinessMatching(Some(reviewDtls))

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching)))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#positions]").html() must include(Messages("error.required.positionWithinBusiness"))
    }

    "submit with valid data with edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positions" -> "05")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SummaryController.get().url))
    }
  }
}
