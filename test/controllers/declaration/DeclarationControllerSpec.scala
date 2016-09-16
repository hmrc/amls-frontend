package controllers.declaration

import config.AMLSAuthConnector
import connectors.{DESConnector, DataCacheConnector}
import models.{ReadStatusResponse, SubscriptionResponse}
import models.declaration.{AddPerson, InternalAccountant}
import org.joda.time.LocalDateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import utils.AuthorisedFixture
import play.api.test.Helpers._
import services.AuthEnrolmentsService
import uk.gov.hmrc.http.cache.client.CacheMap
import play.api.test.Helpers._

import scala.concurrent.Future

class DeclarationControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val declarationController = new DeclarationController {
      override val authConnector = self.authConnector
      override val dataCacheConnector = mock[DataCacheConnector]
      override val desConnector = mock[DESConnector]
      override val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

    val mockCacheMap = mock[CacheMap]

    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "",
      registrationFee = 0,
      fpFee = None,
      premiseFee = 0,
      totalFees = 0,
      paymentReference = ""
    )

    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, false)
    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None, false)

    val addPerson = AddPerson("John", Some("Envy"), "Doe", InternalAccountant)

  }

  "Declaration get" must {

    "use the correct services" in new Fixture {
      DeclarationController.authConnector must be(AMLSAuthConnector)
      DeclarationController.dataCacheConnector must be(DataCacheConnector)
    }

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(declarationController.desConnector.status(any())(any(),any(),any(),any()))
        .thenReturn(Future.successful(notCompletedReadStatusResponse))

      when(declarationController.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(Some("")))

      val result = declarationController.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }

    "load the declaration page for pre-submissions if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      when(declarationController.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(Some("")))

      when(declarationController.desConnector.status(any())(any(),any(),any(),any()))
        .thenReturn(Future.successful(notCompletedReadStatusResponse))

      val result = declarationController.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
    }

    "load the declaration page for amendments if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      when(declarationController.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(Some("")))

      when(declarationController.desConnector.status(any())(any(),any(),any(),any()))
        .thenReturn(Future.successful(pendingReadStatusResponse))

      val result = declarationController.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.amendment.registration"))
    }

    "report error if retrieval of amlsRegNo fails" in new Fixture {
      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      when(declarationController.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(declarationController.desConnector.status(any())(any(),any(),any(),any()))
        .thenReturn(Future.successful(pendingReadStatusResponse))

      val result = declarationController.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
    }

  }

}
