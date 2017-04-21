package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, BusinessFranchiseYes, IdentifySuspiciousActivity}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class IdentifiySuspiciousActivityControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new IdentifySuspiciousActivityController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "IdentifySuspiciousActivityController" when {

    "get is called" must {
      "display the Identify suspicious activity page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))

        page.select("input[type=radio][name=hasWrittenGuidance][value=true]").hasAttr("checked") must be(false)
        page.select("input[type=radio][name=hasWrittenGuidance][value=false]").hasAttr("checked") must be(false)

      }

      "display the identify suspicious activity page with pre populated data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(
            identifySuspiciousActivity = Some(IdentifySuspiciousActivity(true))
          ))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))

        page.select("input[type=radio][name=hasWrittenGuidance][value=true]").hasAttr("checked") must be(true)

      }
    }

    "post is called" must {
      "on post with valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "hasWrittenGuidance" -> "true"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(CacheMap(BusinessActivities.key, Map("" -> Json.obj()))))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.NCARegisteredController.get(false).url))
      }

      "on post with invalid data" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "hasWrittenGuidance" -> "grrrrr"
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("span").html() must include(Messages("error.required.ba.suspicious.activity"))
      }

      "on post with valid data in edit mode" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "hasWrittenGuidance" -> "true"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(CacheMap(BusinessActivities.key, Map("" -> Json.obj()))))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }
    }
  }

  it must {
    "use correct services" in new Fixture {
      IdentifySuspiciousActivityController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
