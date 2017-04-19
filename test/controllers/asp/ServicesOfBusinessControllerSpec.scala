package controllers.asp

import connectors.DataCacheConnector
import models.asp._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ServicesOfBusinessControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ServicesOfBusinessController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "ServicesOfBusinessController" must {

    "on get display Which services does your business provide page" in new Fixture {
      when(controller.dataCacheConnector.fetch[Asp](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("asp.services.title"))
    }

    "submit with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services" -> "02",
        "services" -> "04"
      )

      when(controller.dataCacheConnector.fetch[Asp](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Asp](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.asp.routes.OtherBusinessTaxMattersController.get().url))
    }

    "load the page with data when the user revisits at a later time" in new Fixture {
      when(controller.dataCacheConnector.fetch[Asp](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Asp(Some(ServicesOfBusiness(Set(BookKeeping, Accountancy))), None))))

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

      when(controller.dataCacheConnector.fetch[Asp](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Asp](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("Invalid value")
    }

    "fail submission when no check boxes were selected" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(

      )

      when(controller.dataCacheConnector.fetch[Asp](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Asp](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#services]").html() must include(Messages("error.required.asp.business.services"))
    }

    "submit with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services[1]" -> "02",
        "services[0]" -> "01",
        "services[2]" -> "03"
      )

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))

      when(controller.dataCacheConnector.fetch[Asp](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Asp](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.asp.routes.SummaryController.get().url))
    }

    "go to the date of change page" when {
      "the submission has been approved and registeredOffice has changed" in new Fixture {

        when(controller.dataCacheConnector.fetch[Asp](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))
        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val newRequest = request.withFormUrlEncodedBody(
          "services[0]" -> "02",
          "services[1]" -> "01",
          "services[2]" -> "03")
        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.asp.routes.ServicesOfBusinessDateOfChangeController.get().url))
      }
    }

    "go to the date of change page" when {
      "status is ready for renewal and services selection has changed" in new Fixture {

        when(controller.dataCacheConnector.fetch[Asp](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))
        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(ReadyForRenewal(None)))

        val newRequest = request.withFormUrlEncodedBody(
          "services[0]" -> "02",
          "services[1]" -> "01",
          "services[2]" -> "03")
        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.asp.routes.ServicesOfBusinessDateOfChangeController.get().url))
      }
    }

  }

}
