package controllers.tcsp

import connectors.DataCacheConnector
import models.tcsp._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class TcspTypesControllerSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TcspTypesController {
      override val dataCacheConnector:DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector :AuthConnector =  self.authConnector
    }
  }

  val cacheMap = CacheMap("", Map.empty)

  "TcspTypesController" must {

    "Get:" must {

      "load the Kind of Tcsp are you page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("tcsp.kind.of.service.provider.title"))
      }

      "load the Kind of Tcsp are you page with pre-populated data" in new Fixture {

        val tcspTypes = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc))

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Tcsp(Some(tcspTypes)))))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("tcsp.kind.of.service.provider.title"))
        document.select("input[value=01]").hasAttr("checked") must be(true)
        document.select("input[value=02]").hasAttr("checked") must be(true)
        document.select("input[value=04]").hasAttr("checked") must be(true)
      }
    }

    "Post" must {

      "successfully navigate to next page while storing data in in save4later" in  new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "serviceProviders[]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(cacheMap))

        val result =  controller.post() (newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be (Some(controllers.tcsp.routes.ProvidedServicesController.get().url))

      }

      "successfully navigate to next page while storing data in in save4later in edit mode" in  new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "serviceProviders[]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(cacheMap))

        val result =  controller.post(true) (newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be (Some(controllers.tcsp.routes.SummaryController.get().url))
      }


      "throw error an invalid data entry" in  new Fixture {
        val newrequest = request.withFormUrlEncodedBody(
          "serviceProviders[]" -> "05",
          "onlyOffTheShelfCompsSold" -> "",
          "complexCorpStructureCreation" -> ""
        )

        val result =  controller.post() (newrequest)
        status(result) must be(BAD_REQUEST)
        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#onlyOffTheShelfCompsSold]").text must include(Messages("error.required.tcsp.off.the.shelf.companies"))
        document.select("a[href=#complexCorpStructureCreation]").text must include(Messages("error.required.tcsp.complex.corporate.structures"))
      }
    }
  }
}
