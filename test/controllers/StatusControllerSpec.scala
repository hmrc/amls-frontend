package controllers

import connectors.DataCacheConnector
import models.{Country, SubscriptionResponse}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.registrationprogress.{Completed, NotStarted, Section}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.Helpers._
import services.{LandingService, ProgressService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class StatusControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new StatusController {
      override private[controllers] val landingService: LandingService = mock[LandingService]
      override val authConnector = self.authConnector

      override private[controllers] val progressService: ProgressService = mock[ProgressService]
    }
  }

  "StatusController" should {

    "load the status page" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

      val cacheMap = mock[CacheMap]
      when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
        Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", NotStarted, Call("", "")))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.title() must be(Messages("status.title"))
      document.getElementsByClass("heading-xlarge").first().child(1).html() must be(Messages("status.heading"))
      document.getElementsByClass("heading-secondary").first().html() must be(Messages("summary.status"))
      document.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      document.getElementsByClass("list").first().child(0).html() must be(Messages("status.incomplete"))
      document.getElementsByClass("list").first().child(1).html() must be(Messages("status.notsubmitted"))
      document.getElementsByClass("list").first().child(2).html() must be(Messages("status.feepaid"))
      document.getElementsByClass("list").first().child(3).html() must be(Messages("status.underreview"))
      document.getElementsByClass("list").first().child(4).html() must be(Messages("status.decisionmade"))

      document.getElementsByClass("status-detail").first().child(0).html() must be(Messages("status.incompleteheading"))
      document.getElementsByClass("status-detail").first().child(1).html() must be(Messages("status.description"))
      document.getElementsByClass("status-detail").first().child(2).html() must be(Messages("status.withdraw"))

    }

    "show business name " in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

      val cacheMap = mock[CacheMap]
      when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

      when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(
        Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", NotStarted, Call("", "")))))


      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("panel-indent").first().child(1).html() must be(reviewDtls.businessName)


    }

    "show correct status classes  " when {

      "submission incomplete" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

        val cacheMap = mock[CacheMap]
        when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
          Some(BusinessMatching(Some(reviewDtls), None)))

        when(controller.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", NotStarted, Call("", "")))))


        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("status-list").first().child(0).hasClass("current") must be(true)

        for (index <- 1 to 4) {
          document.getElementsByClass("status-list").first().child(index).hasClass("current") must be(false)
        }

      }

      "submission submitted" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

        val cacheMap = mock[CacheMap]
        when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
          Some(BusinessMatching(Some(reviewDtls), None)))

        when(controller.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, Call("", "")))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("status-list").first().child(0).hasClass("status-list--complete") must be(true)
        document.getElementsByClass("status-list").first().child(1).hasClass("current") must be(true)

        for (index <- 2 to 4) {
          document.getElementsByClass("status-list").first().child(index).hasClass("status-list--upcoming") must be(true)
        }

      }
    }
  }


}
