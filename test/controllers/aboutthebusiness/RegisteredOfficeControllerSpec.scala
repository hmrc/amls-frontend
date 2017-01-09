package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.aboutthebusiness._
import models.status.{SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.data.mapping.Format
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future


class RegisteredOfficeControllerSpec extends PlaySpec with OneAppPerSuite with  MockitoSugar{

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RegisteredOfficeController () {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "RegisteredOfficeController" must {

    "use correct services" in new Fixture {
      RegisteredOfficeController.authConnector must be(AMLSAuthConnector)
      RegisteredOfficeController.dataCacheConnector must be(DataCacheConnector)
    }

    val ukAddress = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "NE7 7DX")

    "load the where is your registered office or main place of business place page" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("aboutthebusiness.registeredoffice.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUK]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("")

    }

    "pre select uk when not in edit mode" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any())).
        thenReturn(Future.successful(Some(AboutTheBusiness(None,None, None, None, None, Some(ukAddress), None))))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUK]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("address line")
    }

    "pre populate where is your registered office or main place of business page with saved data" in new Fixture {

      when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionDecisionRejected))
      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(AboutTheBusiness(None, None, None, None, None, Some(ukAddress), None))))

      val result = controller.get(true)(request)
      status(result) must be(OK)
      contentAsString(result) must include("305")
    }

    "successfully submit form and navigate to target page" in new Fixture {
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))

      when(controller.dataCacheConnector.fetch(any())(any(), any(), any()))
        .thenReturn(Future.successful(None))
      when (controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK"-> "true",
        "addressLine1"->"line1",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"NE7 7DS")
      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ContactingYouController.get().url))
    }

    "fail form submission on validation error" in new Fixture {

      when(controller.dataCacheConnector.fetch(any())(any(), any(), any())).thenReturn(Future.successful(None))
      when (controller.dataCacheConnector.save(any(), any())(any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK"-> "true",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"NE7 7DS")
      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("err.summary"))

    }

    "when it's a variation, go to the date of change page" in new Fixture {
      when(controller.dataCacheConnector.fetch(any())(any(), any(), any()))
        .thenReturn(Future.successful(None))
      when (controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK"-> "true",
        "addressLine1"->"line1",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"NE7 7DS")
      val result = controller.post()(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteredOfficeController.dateOfChange().url))
    }

    "return view for Date of Change" in new Fixture {
      val result = controller.dateOfChange()(request)
      status(result) must be(OK)
    }

    "handle the date of change form post" when {

      "given valid data for a UK address" in new Fixture {

        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.year" -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day" -> "01"
        )

        val office = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "NE7 7DX")
        val updatedOffice = office.copy(dateOfChange = Some(DateOfChange(new LocalDate(2010, 10, 1))))

        val business = AboutTheBusiness(registeredOffice = Some(office))

        when(controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())).
          thenReturn(Future.successful(Some(business)))

        when(controller.dataCacheConnector.save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any[AboutTheBusiness])(any(), any(), any())).
          thenReturn(Future.successful(mock[CacheMap]))

        val result = controller.saveDateOfChange()(postRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))

        val captor = ArgumentCaptor.forClass(classOf[AboutTheBusiness])
        verify(controller.dataCacheConnector).save[AboutTheBusiness](eqTo(AboutTheBusiness.key), captor.capture())(any(), any(), any())

        captor.getValue.registeredOffice match {
          case Some(savedOffice: RegisteredOfficeUK) => savedOffice must be(updatedOffice)
        }

      }

      "given valid data for a Non-UK address" in new Fixture {

        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.year" -> "2005",
          "dateOfChange.month" -> "04",
          "dateOfChange.day" -> "26"
        )

        val office = RegisteredOfficeNonUK("305", "address line", Some("address line2"), Some("address line3"), Country("Finland", "FIN"))
        val updatedOffice = office.copy(dateOfChange = Some(DateOfChange(new LocalDate(2005, 4, 26))))

        val business = AboutTheBusiness(registeredOffice = Some(office))

        when(controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())).
          thenReturn(Future.successful(Some(business)))

        when(controller.dataCacheConnector.save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any[AboutTheBusiness])(any(), any(), any())).
          thenReturn(Future.successful(mock[CacheMap]))

        val result = controller.saveDateOfChange()(postRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))

        val captor = ArgumentCaptor.forClass(classOf[AboutTheBusiness])
        verify(controller.dataCacheConnector).save[AboutTheBusiness](eqTo(AboutTheBusiness.key), captor.capture())(any(), any(), any())

        captor.getValue.registeredOffice match {
          case Some(savedOffice: RegisteredOfficeNonUK) => savedOffice must be(updatedOffice)
        }
      }
    }

    "show the data of change form once again" when {

      "posted with invalid data" in new Fixture {

        val postRequest = request.withFormUrlEncodedBody()

        val result = controller.saveDateOfChange(postRequest)

        status(result) must be(BAD_REQUEST)
        verify(controller.dataCacheConnector, never()).save[AboutTheBusiness](any(), any())(any(), any(), any())

      }

    }
  }
}
