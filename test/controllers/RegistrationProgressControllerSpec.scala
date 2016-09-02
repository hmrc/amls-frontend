package controllers

import connectors.{DataCacheConnector}
import org.jsoup.Jsoup
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, OneAppPerSuite}
import play.api.test.FakeApplication
import services.{ProgressService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture
import play.api.test.Helpers._
import play.api.http.Status.OK
import org.mockito.Mockito._
import org.mockito.Matchers.{any}
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class RegistrationProgressControllerWithAmendmentsSpec extends WordSpec with MustMatchers with OneAppPerSuite with MockitoSugar{

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> true) )

  "RegistrationProgressController" when {

    trait Fixture extends AuthorisedFixture {
      self =>
      val controller = new RegistrationProgressController {
        override val authConnector = self.authConnector
        override protected[controllers] def service: ProgressService = mock[ProgressService]
        override protected[controllers] def dataCache: DataCacheConnector = mock[DataCacheConnector]
      }

      val mockCacheMap = mock[CacheMap]

      when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
    }

    "there has already been a submission" must {
      "show the update your information page" in new Fixture {
         val responseF = controller.get()(request)
        status(responseF) must be (OK)
        Jsoup.parse(contentAsString(responseF)).title must be ("glkdfjlgj")
      }
    }

    "there has not already been a submission" must {
      "show the registration page" in new Fixture {
        val responseF = controller.get()(request)
        status(responseF) must be (OK)
        Jsoup.parse(contentAsString(responseF)).title must be ("gdfgdfdsgdf")
      }
    }
  }
}

class RegistrationProgressControllerWithoutAmendmentsSpec extends WordSpec with MustMatchers with OneAppPerSuite{

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> false) )


  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new RegistrationProgressController {
      override val authConnector = self.authConnector
      override protected[controllers] def service: ProgressService = mock[ProgressService]
      override protected[controllers] def dataCache: DataCacheConnector = mock[DataCacheConnector]
    }

    val mockCacheMap = mock[CacheMap]

    when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
      .thenReturn(Future.successful(Some(mockCacheMap)))
  }

  "RegistrationProgressController" when {
    "there has already been a submission" must {
      "show the registration progress page" in new Fixture {
        val responseF = controller.get()(request)
        status(responseF) must be (OK)
        Jsoup.parse(contentAsString(responseF)).title must be ("glkdfjlgj")

        1 must be (2)
      }
    }
  }
}
