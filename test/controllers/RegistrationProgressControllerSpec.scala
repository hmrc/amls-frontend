package controllers

import connectors.{DataCacheConnector}
import models.SubscriptionResponse
import models.registrationprogress.Section
import org.jsoup.Jsoup
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, OneAppPerSuite}
import play.api.test.FakeApplication
import services.{AuthEnrolmentsService, ProgressService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture
import play.api.test.Helpers._
import play.api.http.Status.OK
import org.mockito.Mockito._
import org.mockito.Matchers.{any}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}

import scala.concurrent.{ExecutionContext, Future}

trait Fixture extends AuthorisedFixture {
  self =>
  val controller = new RegistrationProgressController {
    override val authConnector = self.authConnector
    override protected[controllers] val service: ProgressService = mock[ProgressService]
    override protected[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
    override protected[controllers] val enrolmentsService : AuthEnrolmentsService = mock[AuthEnrolmentsService]
  }

  protected val mockCacheMap = mock[CacheMap]

  when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
    .thenReturn(Future.successful(Some(mockCacheMap)))

  when(controller.service.sections(mockCacheMap))
    .thenReturn(Seq.empty[Section])


  when(controller.service.sections(any[HeaderCarrier], any[AuthContext], any[ExecutionContext]))
    .thenReturn(Future.successful(Seq.empty[Section]))
}

class RegistrationProgressControllerWithAmendmentsSpec extends WordSpec with MustMatchers with OneAppPerSuite with MockitoSugar{

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> true) )

  "RegistrationProgressController" when {
    "the user is enrolled into the AMLS Account" must {
      "show the update your information page" in new Fixture {

        when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some("AMLSREFNO")))

        val responseF = controller.get()(request)
        status(responseF) must be (OK)
        Jsoup.parse(contentAsString(responseF)).title must be ("Update your information - Your application - Anti-money laundering registration - GOV.UK")
      }
    }

    "the user is not enrolled into the AMLS Account" must {
      "show the registration progress page" in new Fixture {
        when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(None))

        val responseF = controller.get()(request)
        status(responseF) must be (OK)
        Jsoup.parse(contentAsString(responseF)).title must be ("Application progress – Anti-money laundering supervision - GOV.UK")
      }
    }
  }
}

class RegistrationProgressControllerWithoutAmendmentsSpec extends WordSpec with MustMatchers with OneAppPerSuite{

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> false) )

  "RegistrationProgressController" when {
    "there has already been a submission" must {
      "show the registration progress page" in new Fixture {
        when(mockCacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(SubscriptionResponse("FRMBNDLENO", "AMLSREFNO", 120, None, 12, 134, "PAYREF")))

        val responseF = controller.get()(request)
        status(responseF) must be (OK)
        Jsoup.parse(contentAsString(responseF)).title must be ("Application progress – Anti-money laundering supervision - GOV.UK")
      }
    }
  }
}
