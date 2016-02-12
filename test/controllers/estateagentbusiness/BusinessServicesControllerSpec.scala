package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.estateagentbusiness._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class BusinessServicesControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {
  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new BusinessServicesController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "BusinessServicesController" must {

    "use correct services" in new Fixture {
      BusinessServicesController.authConnector must be(AMLSAuthConnector)
      BusinessServicesController.dataCacheConnector must be(DataCacheConnector)
    }

    "on get display Business services page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("estateagentbusiness.servicess.title"))
    }

    "submit with valid data 1" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services" -> "02",
        "services" -> "08"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.PenalisedUnderEstateAgentsActController.get().url))
    }

    "load the page with data when the user revisits at a later time" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(EstateAgentBusiness(Some(Services(Set(Auction, Residential))), None, None, None))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[value=03]").hasAttr("checked") must be(true)
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "fail submission on error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services" -> "0299999"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("Invalid value")
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#services[0].services]").html() must include("Invalid value")
    }

    "fail submission when no check boxes were selected" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(

      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("This field is required")
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#services]").html() must include("This field is required")
    }


    "submit with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services[1]" -> "02",
        "services[0]" -> "01",
        "services[2]" -> "03"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.SummaryController.get().url))
    }

    "submit with valid data with Residential option from business services" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services[0]" -> "01",
        "services[1]" -> "02",
        "services[2]" -> "03"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ResidentialRedressSchemeController.get().url))
    }

    "submit with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services[0]" -> "02",
        "services[1]" -> "08"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.PenalisedUnderEstateAgentsActController.get().url))
    }
  }
}
