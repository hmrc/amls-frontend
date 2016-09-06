package controllers.businessactivities

import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, TaxMatters}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class TaxMattersControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TaxMattersController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "TaxMattersController" must {

    "use correct services" in new Fixture {
      TaxMattersController.dataCacheConnector must be(DataCacheConnector)
    }

    "on get, display the 'Manage Your Tax Affairs?' page" in new Fixture {
      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessactivities.tax.matters.title"))
    }

    "on get, display the 'Manage Your Tax Affairs?' page with pre populated data if found in cache" in new Fixture {

      when(
        controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any())
      ).thenReturn(
        Future.successful(Some(BusinessActivities(taxMatters = Some(TaxMatters(true)))))
      )
      val result = controller.get()(request)

      status(result) must be(OK)

      val page = Jsoup.parse(contentAsString(result))

      page.select("input[type=radio][name=manageYourTaxAffairs][value=true][checked]").size must be (1)
      page.select("input[type=radio][name=manageYourTaxAffairs][value=false]").size must be (1)
      page.select("input[type=radio][name=manageYourTaxAffairs][value=false][checked]").size must be (0)

    }

    "redirect to Check Your Answers on post with valid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "manageYourTaxAffairs" -> "true"
      )

      when(
        controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any())
      ).thenReturn(Future.successful(None))
      when(
        controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(CacheMap(BusinessActivities.key, Map("" -> Json.obj())))
      )

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "respond with Bad Request on post with invalid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "manageYourTaxAffairs" -> "grrrrr"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document  = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include(Messages("error.required.ba.tax.matters"))
    }

    "redirect to Check Your Answers on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "manageYourTaxAffairs" -> "true"
      )

      when(
        controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any())
      ).thenReturn(Future.successful(None))

      when(
        controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(CacheMap(BusinessActivities.key, Map("" -> Json.obj())))
      )

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }
  }
}
