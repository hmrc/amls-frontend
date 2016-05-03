package controllers.tcsp

import connectors.DataCacheConnector
import models.tcsp.{Other, ProvidedServices, Tcsp}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import org.mockito.Mockito._

import scala.concurrent.Future

class ProvidedServicesControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new ProvidedServicesController {
      override val authConnector = self.authConnector
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
    }
  }

  "ProvidedServicesController" must {

    "get" must {

      "load the provided services page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Tcsp](any())(any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("tcsp.provided_services.title"))
      }

      "load the provided services page with existing data" in new Fixture {

        val tcsp = Tcsp(providedServices = Some(ProvidedServices(Set(Other("some other service")))))
        when(controller.dataCacheConnector.fetch[Tcsp](any())(any(), any(), any())) thenReturn Future.successful(Some(tcsp))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.title() must be (Messages("tcsp.provided_services.title"))

        document.select("input[id=services-08]").attr("checked") must be("checked")
        document.select("input[name=details]").`val` must be ("some other service")
      }
    }

    "post" must {

      val cacheMap = mock[CacheMap]

      "successfully navigate to next page when valid data is sent" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "services[]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any())(any(), any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any())(any(), any(), any())).thenReturn(Future.successful(cacheMap))

        val result = controller.post()(newRequest)

        status(result) must be (SEE_OTHER)
        redirectLocation(result) must be (Some(controllers.tcsp.routes.ServicesOfAnotherTCSPController.get().url))

      }

      "successfully navigate to summary page when valid data is sent and edit mode is on" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "services[]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any())(any(), any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any())(any(), any(), any())).thenReturn(Future.successful(cacheMap))

        val result = controller.post(true)(newRequest)

        status(result) must be (SEE_OTHER)
        //TODO: Update this test to new view.
        redirectLocation(result) must be (Some(controllers.tcsp.routes.ProvidedServicesController.get().url))

      }

      "show an error when services data not sent" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody()

        val result = controller.post()(newRequest)

        status(result) must be (BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#services]").text must include(Messages("error.required.tcsp.provided_services.services"))
      }


      "show an error when other data not sent" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "services[]" -> "08",
          "details" -> ""
        )

        val result = controller.post(true)(newRequest)

        status(result) must be (BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#details]").text must include(Messages("error.required.tcsp.provided_services.details"))

      }

    }


  }
}
