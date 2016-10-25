package controllers

import connectors.DataCacheConnector
import models.SubscriptionResponse
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, NotStarted, Section}
import org.jsoup.Jsoup
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, OneServerPerSuite}
import play.api.mvc.Call
import play.api.test.FakeApplication
import services.{AuthEnrolmentsService, ProgressService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.AuthorisedFixture
import play.api.test.Helpers._
import play.api.http.Status.OK
import org.mockito.Mockito._
import org.mockito.Matchers.any
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import play.api.i18n.Messages

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
}

class RegistrationProgressControllerWithAmendmentsSpec extends WordSpec with MustMatchers with OneAppPerSuite with MockitoSugar{

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> true) )

  "RegistrationProgressController" when {
    "the user is enrolled into the AMLS Account" must {
      "show the update your information page" in new Fixture {
        val complete = mock[BusinessMatching]
        when(complete.isComplete) thenReturn true

        when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some("AMLSREFNO")))

        when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.service.sections(mockCacheMap))
          .thenReturn(Seq.empty[Section])

        when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

        val responseF = controller.get()(request)
        status(responseF) must be (OK)
        val pageTitle = Messages("amendment.title") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")
        Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
      }
    }

    "all sections are complete and" when {
      "a section has changed" must {
        "enable the submission button" in new Fixture{
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.service.sections(mockCacheMap))
            .thenReturn(Seq(
              Section("TESTSECTION1", Completed, false, mock[Call]),
              Section("TESTSECTION2", Completed, true, mock[Call])
            ))

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val responseF = controller.get()(request)
          status(responseF) must be (OK)
          val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
          submitButtons.size() must be (1)
          submitButtons.first().hasAttr("disabled") must be (false)
        }
      }


      "no section has changed" must {
        "disable the submission button" in new Fixture{
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.service.sections(mockCacheMap))
            .thenReturn(Seq(
              Section("TESTSECTION1", Completed, false, mock[Call]),
              Section("TESTSECTION2", Completed, false, mock[Call])
            ))

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))


          val responseF = controller.get()(request)
          status(responseF) must be (OK)
          val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
          submitButtons.size() must be (1)
          submitButtons.first().hasAttr("disabled") must be (true)
        }
      }
    }

    "some sections are not complete and" when {
      "a section has changed" must {
        "disable the submission button" in new Fixture {
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.service.sections(mockCacheMap))
            .thenReturn(Seq(
              Section("TESTSECTION1", NotStarted, false, mock[Call]),
              Section("TESTSECTION2", Completed, true, mock[Call])
            ))

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val responseF = controller.get()(request)
          status(responseF) must be (OK)
          val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
          submitButtons.size() must be (1)
          submitButtons.first().hasAttr("disabled") must be (true)
        }
      }

      "no section has changed" must {
        "disable the submission button" in new Fixture {
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.service.sections(mockCacheMap))
            .thenReturn(Seq(
              Section("TESTSECTION1", NotStarted, false, mock[Call]),
              Section("TESTSECTION2", Completed, false, mock[Call])
            ))

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val responseF = controller.get()(request)
          status(responseF) must be (OK)
          val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
          submitButtons.size() must be (1)
          submitButtons.first().hasAttr("disabled") must be (true)
        }
      }
    }

    "the user is not enrolled into the AMLS Account" must {
      "show the registration progress page" in new Fixture {
        val complete = mock[BusinessMatching]
        when(complete.isComplete) thenReturn true

        when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.service.sections(mockCacheMap))
          .thenReturn(Seq.empty[Section])

        when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(None))

        when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

        val responseF = controller.get()(request)
        status(responseF) must be (OK)
        val pageTitle = Messages("progress.title") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")
        Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
      }
    }

    "pre application must throw an exception" when {
      "the business matching is incomplete" in new Fixture {
        val cachmap = mock[CacheMap]
        val complete = mock[BusinessMatching]
        val emptyCacheMap = mock[CacheMap]


        when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(cachmap)))

       when(complete.isComplete) thenReturn false
        when(cachmap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.LandingController.get().url)
      }
    }
  }
}

class RegistrationProgressControllerWithoutAmendmentsSpec extends WordSpec with MustMatchers with OneAppPerSuite{

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> false) )

  "RegistrationProgressController" when {
    "there has already been a submission" must {
      "show the registration progress page" in new Fixture {
        when(controller.service.sections(any[HeaderCarrier], any[AuthContext], any[ExecutionContext]))
          .thenReturn(Future.successful(Seq(
            Section("TESTSECTION1", Completed, false, mock[Call]),
            Section("TESTSECTION2", Completed, false, mock[Call])
          )))

        val responseF = controller.get()(request)
        status(responseF) must be (OK)
        val pageTitle = Messages("progress.title") + " - " +
          Messages("title.yapp") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")
        Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
      }
    }
  }
}
