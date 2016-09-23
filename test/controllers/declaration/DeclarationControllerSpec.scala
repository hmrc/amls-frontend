package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.declaration.{AddPerson, InternalAccountant}
import models.status.NotCompleted
import models.{ReadStatusResponse, SubscriptionResponse}
import org.joda.time.LocalDateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class DeclarationControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val declarationController = new DeclarationController {
      override val authConnector = self.authConnector
      override val dataCacheConnector = mock[DataCacheConnector]
      override val statusService = mock[StatusService]
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
      DeclarationController.statusService must be (StatusService)
    }

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(declarationController.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(NotCompleted))

      val result = declarationController.get()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }

    "load the declaration page for pre-submissions if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
    }

    "load the declaration for amendments page for submissions if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

     val result = declarationController.getWithAmendment()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.amendment.application"))
      contentAsString(result) must include(Messages("declaration.declaration.amendment.title"))
    }

    "report error if retrieval of amlsRegNo fails" in new Fixture {
      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
      contentAsString(result) must include(Messages("declaration.declaration.title"))
    }

  }

}

class DeclarationControllerAmendmentSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> false) )

  trait Fixture extends AuthorisedFixture {
    self =>

    val declarationController = new DeclarationController {
      override val authConnector = self.authConnector
      override val dataCacheConnector = mock[DataCacheConnector]
      override val statusService = mock[StatusService]
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
    "load the declaration for amendments page for submissions if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
      contentAsString(result) must include(Messages("declaration.declaration.title"))
    }
  }
}
