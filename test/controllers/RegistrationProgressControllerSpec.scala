/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import connectors.DataCacheConnector
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, NotStarted, Section}
import models.renewal.{InvolvedInOtherNo, Renewal}
import models.responsiblepeople._
import models.status._
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
import org.mockito.Matchers.{eq => eqTo, _}


class RegistrationProgressControllerSpec extends GenericTestHelper with MustMatchers with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.partner" -> true))

  trait Fixture extends AuthorisedFixture {self =>
    val request = addToken(authRequest)
    val controller = new RegistrationProgressController {
      override val authConnector = self.authConnector
      override protected[controllers] val progressService: ProgressService = mock[ProgressService]
      override protected[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override protected[controllers] val enrolmentsService : AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override protected[controllers] val statusService : StatusService = mock[StatusService]
    }

    protected val mockCacheMap = mock[CacheMap]

    when(controller.statusService.getStatus(any(), any(), any()))
      .thenReturn(Future.successful(SubmissionReady))

    when(controller.dataCache.fetch[Renewal](any())(any(), any(), any()))
      .thenReturn(Future.successful(None))
  }


  "RegistrationProgressController" when {
    "get is called" when {
      "the user is enrolled into the AMLS Account" must {
        "show the update your information page" in new Fixture {
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(Seq.empty[Section])

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val responseF = controller.get()(request)
          status(responseF) must be(OK)
          val pageTitle = Messages("amendment.title") + " - " +
            Messages("title.yapp") + " - " +
            Messages("title.amls") + " - " + Messages("title.gov")
          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
        }
      }

      "redirect to renewal registration progress" when {
        "status is ready for renewal and" must {
          "renewal data exists in save4later" in new Fixture {
            when(controller.dataCache.fetch[Renewal](any())(any(), any(), any())).thenReturn(Future.successful(Some(Renewal(Some(InvolvedInOtherNo)))))
            when(controller.statusService.getStatus(any(), any(), any()))
              .thenReturn(Future.successful(ReadyForRenewal(None)))

            val responseF = controller.get()(request)
            status(responseF) must be(SEE_OTHER)
            redirectLocation(responseF) must be(Some(renewal.routes.RenewalProgressController.get().url))
          }
        }
      }

      "redirect to renewal registration progress" when {
        "status is ready for renewal submitted" must {
          "renewal data exists in save4later" in new Fixture {
            when(controller.dataCache.fetch[Renewal](any())(any(), any(), any())).thenReturn(Future.successful(Some(Renewal(Some(InvolvedInOtherNo)))))
            when(controller.statusService.getStatus(any(), any(), any()))
              .thenReturn(Future.successful(RenewalSubmitted(None)))

            val responseF = controller.get()(request)
            status(responseF) must be(SEE_OTHER)
            redirectLocation(responseF) must be(Some(renewal.routes.RenewalProgressController.get().url))
          }
        }
      }

      "redirect to registration progress" when {
        "status is ready for renewal and" must {
          "redirectWithNominatedOfficer" in new Fixture {

            when(controller.statusService.getStatus(any(), any(), any()))
              .thenReturn(Future.successful(ReadyForRenewal(None)))

            val complete = mock[BusinessMatching]
            when(complete.isComplete) thenReturn true

            when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq.empty[Section])

            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(None))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

            val responseF = controller.get()(request)
            status(responseF) must be(OK)
            val pageTitle = Messages("progress.title") + " - " +
              Messages("title.yapp") + " - " +
              Messages("title.amls") + " - " + Messages("title.gov")
            Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle

          }
        }
      }

      "all sections are complete and" when {
        "a section has changed" must {
          "enable the submission button" in new Fixture {
            val complete = mock[BusinessMatching]
            when(complete.isComplete) thenReturn true

            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some("AMLSREFNO")))

            when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq(
                Section("TESTSECTION1", Completed, false, mock[Call]),
                Section("TESTSECTION2", Completed, true, mock[Call])
              ))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

            val responseF = controller.get()(request)
            status(responseF) must be(OK)
            val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
            submitButtons.size() must be(1)
            submitButtons.first().hasAttr("disabled") must be(false)
          }
        }


        "no section has changed" must {
          "disable the submission button" in new Fixture {
            val complete = mock[BusinessMatching]
            when(complete.isComplete) thenReturn true

            when(controller.statusService.getStatus(any(), any(), any()))
              .thenReturn(Future.successful(SubmissionReadyForReview))

            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some("AMLSREFNO")))

            when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq(
                Section("TESTSECTION1", Completed, false, mock[Call]),
                Section("TESTSECTION2", Completed, false, mock[Call])
              ))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))


            val responseF = controller.get()(request)
            status(responseF) must be(OK)
            val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
            submitButtons.size() must be(1)
            submitButtons.first().hasAttr("disabled") must be(true)
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

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq(
                Section("TESTSECTION1", NotStarted, false, mock[Call]),
                Section("TESTSECTION2", Completed, true, mock[Call])
              ))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

            val responseF = controller.get()(request)
            status(responseF) must be(OK)
            val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
            submitButtons.size() must be(1)
            submitButtons.first().hasAttr("disabled") must be(true)
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

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq(
                Section("TESTSECTION1", NotStarted, false, mock[Call]),
                Section("TESTSECTION2", Completed, false, mock[Call])
              ))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

            val responseF = controller.get()(request)
            status(responseF) must be(OK)
            val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
            submitButtons.size() must be(1)
            submitButtons.first().hasAttr("disabled") must be(true)
          }
        }
      }

      "exclude business matching section from registration page" when {
        "status is other then NotCompleted and SubmissionReady" in new Fixture {
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val sections = Seq(
            Section(BusinessMatching.messageKey, Completed, false, mock[Call]),
            Section("TESTSECTION2", Completed, false, mock[Call])
          )

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(sections)

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val responseF = controller.get()(request)
          status(responseF) must be(OK)
          val doc = Jsoup.parse(contentAsString((responseF)))
          doc.getElementsMatchingOwnText(Messages("amendment.text.1")).hasText must be(true)
          val elements = doc.getElementsMatchingOwnText(Messages("progress.visuallyhidden.view.amend"))
          elements.size() must be(sections.size - 1)

        }
      }

      "include business matching section in registration page" when {
        "status is SubmissionReady" in new Fixture {
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val sections = Seq(
            Section(BusinessMatching.messageKey, Completed, false, mock[Call]),
            Section("TESTSECTION2", Completed, false, mock[Call])
          )

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(sections)

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val responseF = controller.get()(request)
          status(responseF) must be(OK)
          val doc = Jsoup.parse(contentAsString((responseF)))
          val elements = doc.getElementsMatchingOwnText(Messages("progress.visuallyhidden.completed"))
          elements.size() must be(sections.size)

        }
      }

      "include business matching section in registration page" when {
        "status is NotCompleted" in new Fixture {
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          val sections = Seq(
            Section(BusinessMatching.messageKey, Completed, false, mock[Call]),
            Section("TESTSECTION2", Completed, false, mock[Call])
          )

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(sections)

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val responseF = controller.get()(request)
          status(responseF) must be(OK)
          val doc = Jsoup.parse(contentAsString((responseF)))
          val elements = doc.getElementsMatchingOwnText(Messages("progress.visuallyhidden.completed"))
          elements.size() must be(sections.size)

        }
      }
      "the user is not enrolled into the AMLS Account" must {
        "show the registration progress page" in new Fixture {
          val complete = mock[BusinessMatching]
          when(complete.isComplete) thenReturn true

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(Seq.empty[Section])

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(None))

          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val responseF = controller.get()(request)
          status(responseF) must be(OK)
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
    }

    "post is called" when {
      "ensure getSubmitRedirect is called" in new Fixture{
        verify(controller.progressService).getSubmitRedirect(any(), any(), any())
      }
    }
  }
}
