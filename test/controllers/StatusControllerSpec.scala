package controllers

import connectors.DataCacheConnector
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class StatusControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar{

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new StatusController {
      override private[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "StatusController" should {

    "load the status page" in new Fixture{

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.title() must be(Messages("status.title"))
      document.getElementsByClass("heading-xlarge").first().child(1).html() must be(Messages("status.heading"))
      document.getElementsByClass("heading-secondary").first().html() must be(Messages("summary.status"))
      document.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      document.getElementsByClass("list").first().child(0).html() must be (Messages("status.incomplete"))
      document.getElementsByClass("list").first().child(1).html() must be (Messages("status.submitted"))
      document.getElementsByClass("list").first().child(2).html() must be (Messages("status.feepaid"))
      document.getElementsByClass("list").first().child(3).html() must be (Messages("status.underreview"))
      document.getElementsByClass("list").first().child(4).html() must be (Messages("status.decisionmade"))

      document.getElementsByClass("status-detail").first().child(0).html() must be (Messages("status.incompleteheading"))
      document.getElementsByClass("status-detail").first().child(1).html() must be (Messages("status.description"))
      document.getElementsByClass("status-detail").first().child(2).html() must be (Messages("status.withdraw"))

    }

    "show business name " in new Fixture{

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(Some(reviewDtls), None))))


      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("panel-indent").first().child(1).html() must be(reviewDtls.businessName)


    }
  }



}
