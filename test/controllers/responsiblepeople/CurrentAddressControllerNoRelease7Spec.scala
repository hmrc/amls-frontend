package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future
class CurrentAddressControllerNoRelease7Spec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val recordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val currentAddressController = new CurrentAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> false))

  val emptyCache = CacheMap("", Map.empty)

  "CurrentAddressController" must {
    "when the service status is Approved and the address is changed" when {
      "time at address is less than 1 year" must {
        "redirect to the AdditionalAddressController" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "new Line 1",
            "addressLine2" -> "new Line 2",
            "postCode" -> "NE17YH"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history), lineId = Some(1))


          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = currentAddressController.post(recordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtAddressController.get(recordId, true).url))
        }
      }
      "time at address is more than 1 year" must {
        "redirect to the correct location" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "new Line 1",
            "addressLine2" -> "new Line 2",
            "postCode" -> "NE17YH"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(OneToThreeYears))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history), lineId = Some(1))


          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = currentAddressController.post(recordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(recordId, true).url))
        }
      }
    }
  }
}
