package controllers

import connectors.DataCacheConnector
import controllers.aboutyou.RoleWithinBusinessController
import models.RoleWithinBusiness
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class RoleWithinBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val request = FakeRequest()

  object Controller extends RoleWithinBusinessController {
    override val authConnector = mock[AuthConnector]
    override val dataCacheConnector = mock[DataCacheConnector]
  }

  "RoleWithinBusinessController" must {

    "on get display the Role Within Business page" in {

      when(Controller.dataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](any())
        (any(), any(),  any())).thenReturn(Future.successful(None))

      val result = Controller.get(mock[AuthContext], request)
      status(result) must be(OK)
      contentAsString(result) must include("What is your role within the business?")
    }

    "on get display the Role Within Business page with pre populated data" in {

      val roleWithinBusiness = RoleWithinBusiness("01", "")

      when(Controller.dataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](any())
        (any(), any(),  any())).thenReturn(Future.successful(Some(roleWithinBusiness)))

      val result = Controller.get(mock[AuthContext], request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "on post with valid data" in {

      val request = FakeRequest().withFormUrlEncodedBody(
        "roleWithinBusiness" -> "01",
        "other" -> ""
      )

      when(Controller.dataCacheConnector.saveDataShortLivedCache[RoleWithinBusiness](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      val result = Controller.post(mock[AuthContext], request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutyou.routes.YourDetailsController.get().url))
    }

    "on post with invalid data" in {

      val request = FakeRequest().withFormUrlEncodedBody(
        "other" -> "foo"
      )

      val result = Controller.post(mock[AuthContext], request)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=other]").`val`() must be("foo")
    }
  }
}
