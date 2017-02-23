package controllers

import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, NotStarted, Section}
import models.responsiblepeople._
import models.status.{SubmissionDecisionApproved, SubmissionReady}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Call
import play.api.test.FakeApplication
import services.{AuthEnrolmentsService, ProgressService, StatusService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}
import play.api.test.Helpers._
import play.api.http.Status.OK
import org.mockito.Mockito._
import org.mockito.Matchers.any
import uk.gov.hmrc.http.cache.client.CacheMap
import play.api.i18n.Messages

import scala.concurrent.{ExecutionContext, Future}


class RegistrationProgressControllerWithAmendmentsSpec extends GenericTestHelper with MustMatchers with MockitoSugar{
  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new RegistrationProgressController {
      override val authConnector = self.authConnector
      override protected[controllers] val service: ProgressService = mock[ProgressService]
      override protected[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override protected[controllers] val enrolmentsService : AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override protected[controllers] val statusService : StatusService = mock[StatusService]
    }

    protected val mockCacheMap = mock[CacheMap]
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> true) )

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
          Messages("title.yapp") + " - " +
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
          Messages("title.yapp") + " - " +
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

    "redirect to 'Who is registering this business?'" when {

      "at least one of the person in responsible people is nominated officer" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val john = ResponsiblePeople(Some(PersonName("John", Some("Alan"), "Smith", None, None)), None, None, None, Some(positions))
        val mark = ResponsiblePeople(Some(PersonName("Mark", None, "Smith", None, None)), None, None, None, Some(positions))
        val respinsiblePeople = Seq(john, mark)

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))
        when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(),any()))
          .thenReturn(Future.successful(Some(respinsiblePeople)))
        val result = controller.post()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsRegisteringController.get().url)
      }

      "at least one of the person in responsible people is nominated officer and status id amendment" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val john = ResponsiblePeople(Some(PersonName("John", Some("Alan"), "Smith", None, None)), None, None, None, Some(positions))
        val mark = ResponsiblePeople(Some(PersonName("Mark", None, "Smith", None, None)), None, None, None, Some(positions))
        val respinsiblePeople = Seq(john, mark)

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))
        when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(),any()))
          .thenReturn(Future.successful(Some(respinsiblePeople)))
        val result = controller.post()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsRegisteringController.getWithAmendment().url)
      }

      "no respnsible people" in new Fixture {
        when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(),any()))
          .thenReturn(Future.successful(None))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))
        val result = controller.post()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.get().url)
      }
    }

    "redirect to 'Who is the business’s nominated officer?'" when {
      "no one is nominated officer in responsible people" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
        val john = ResponsiblePeople(Some(PersonName("John", Some("Alan"), "Smith", None, None)), None, None, None, Some(positions))
        val mark = ResponsiblePeople(Some(PersonName("Mark", None, "Smith", None, None)), None, None, None, Some(positions))
        val respinsiblePeople = Seq(john, mark)

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(),any())).
          thenReturn(Future.successful(Some(respinsiblePeople)))
        val result = controller.post()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.get().url)
      }

      "no one is nominated officer in responsible people and status is amendment" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
        val john = ResponsiblePeople(Some(PersonName("John", Some("Alan"), "Smith", None, None)), None, None, None, Some(positions))
        val mark = ResponsiblePeople(Some(PersonName("Mark", None, "Smith", None, None)), None, None, None, Some(positions))
        val respinsiblePeople = Seq(john, mark)

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(),any())).
          thenReturn(Future.successful(Some(respinsiblePeople)))
        val result = controller.post()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment().url)
      }
    }
  }
}

class RegistrationProgressControllerWithoutAmendmentsSpec extends GenericTestHelper with MustMatchers {
  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new RegistrationProgressController {
      override val authConnector = self.authConnector
      override protected[controllers] val service: ProgressService = mock[ProgressService]
      override protected[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override protected[controllers] val enrolmentsService : AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override protected[controllers] val statusService : StatusService = mock[StatusService]
    }

    protected val mockCacheMap = mock[CacheMap]
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> false) )

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
