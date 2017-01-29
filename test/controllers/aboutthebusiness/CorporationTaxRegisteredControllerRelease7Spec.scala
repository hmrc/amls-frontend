package controllers.aboutthebusiness

import connectors.{BusinessMatchingConnector, BusinessMatchingReviewDetails, DataCacheConnector}
import models.aboutthebusiness.{AboutTheBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future
class CorporationTaxRegisteredControllerRelease7Spec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new CorporationTaxRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val businessMatchingConnector = mock[BusinessMatchingConnector]
    }
  }

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map(
    "Test.microservice.services.feature-toggle.business-matching-details-lookup" -> true
  ))


  val emptyCache = CacheMap("", Map.empty)

  "CorporationTaxRegisteredControllerRelease7Spec" must {

    "on get retrieve the corporation tax reference from business customer api if no previous entry and feature flag is high" in new Fixture {

      val reviewDetailsModel = mock[BusinessMatchingReviewDetails]
      when(reviewDetailsModel.utr) thenReturn Some("0987654321")

      when(controller.businessMatchingConnector.getReviewDetails(any())) thenReturn Future.successful(Some(reviewDetailsModel))

      val data = AboutTheBusiness(corporationTaxRegistered = None)

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(data)))

      val result = controller.get()(request)

      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("registeredForCorporationTax-true").hasAttr("checked") must be(true)
      document.getElementById("corporationTaxReference").`val` must be("0987654321")
    }

  }
}
