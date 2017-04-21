package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.estateagentbusiness._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BusinessServicesControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new BusinessServicesController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessServicesController" must {

    "use correct services" in new Fixture {
      BusinessServicesController.authConnector must be(AMLSAuthConnector)
      BusinessServicesController.dataCacheConnector must be(DataCacheConnector)
    }

    "on get display Business services page" in new Fixture {
      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("estateagentbusiness.services.title") + " - " + Messages("summary.estateagentbusiness") + " - " + Messages("title.amls") + " - " + Messages("title.gov"))
    }

    "submit with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services[0]" -> "02",
        "services[1]" -> "08"
      )
      val eab = EstateAgentBusiness(Some(Services(Set(Residential))), Some(ThePropertyOmbudsman), None, None)

      val eabWithoutRedress = EstateAgentBusiness(Some(Services(Set(Commercial, Development),None)),None,None,None,true)

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))

      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(eab)))

      when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), meq(Some(eabWithoutRedress)))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.PenalisedUnderEstateAgentsActController.get().url))
    }

    "load the page with data when the user revisits at a later time" in new Fixture {
      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
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

      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("Invalid value")
    }

    "fail submission when no check boxes were selected" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(

      )

      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#services]").html() must include(Messages("error.required.eab.business.services"))
    }


    "submit with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services[1]" -> "02",
        "services[0]" -> "01",
        "services[2]" -> "03"
      )
      val eab = EstateAgentBusiness(Some(Services(Set(Auction, Commercial, Residential))), None, None, None)
      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(eab)))

      when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), meq(Some(eab)))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ResidentialRedressSchemeController.get(true).url))
    }

    "submit with valid data with Residential option from business services" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services[0]" -> "01",
        "services[1]" -> "02",
        "services[2]" -> "03"
      )

      val eab = EstateAgentBusiness(Some(Services(Set(Auction, Commercial, Residential))), Some(ThePropertyOmbudsman), None, None)

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(eab)))

      when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), meq(Some(eab)))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ResidentialRedressSchemeController.get().url))
    }


    "successfully redirect to dateOfChange page" when {
      "user edits services option" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "services[0]" -> "01",
          "services[1]" -> "02",
          "services[2]" -> "07"
        )

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(EstateAgentBusiness(
          services = Some(Services(Set(Residential, Commercial, Auction)))))))

        when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ServicesDateOfChangeController.get().url))
      }
    }

    "successfully redirect to dateOfChange page" when {
      "status is ready for renewal and user edits services option" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "services[0]" -> "01",
          "services[1]" -> "02",
          "services[2]" -> "07"
        )

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(ReadyForRenewal(None)))

        when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(EstateAgentBusiness(
          services = Some(Services(Set(Residential, Commercial, Auction)))))))

        when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ServicesDateOfChangeController.get().url))
      }
    }
  }
}
