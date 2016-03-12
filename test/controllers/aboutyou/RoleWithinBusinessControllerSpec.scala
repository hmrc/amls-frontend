package controllers

import connectors.DataCacheConnector
import controllers.aboutyou.RoleWithinBusinessController
import models.aboutyou.{AboutYou, BeneficialShareholder, RoleWithinBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class RoleWithinBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RoleWithinBusinessController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "RoleWithinBusinessController" must {

    "on get display the Role Within Business page" in new Fixture {

      when(controller.dataCacheConnector.fetch[RoleWithinBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("What is your role within the business?")
    }

    "on get display the Role Within Business page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutYou](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(AboutYou(None, Some(BeneficialShareholder)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      // TODO
//      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "roleWithinBusiness" -> "01",
        "other" -> ""
      )

      when(controller.dataCacheConnector.fetch[AboutYou](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[AboutYou](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutyou.routes.YourDetailsController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "roleWithinBusiness" -> "01",
        "other" -> ""
      )

      when(controller.dataCacheConnector.fetch[AboutYou](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[AboutYou](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutyou.routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "other" -> "foo"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      // TODO
//      document.select("input[name=other]").`val` must be("foo")
    }
  }
}
