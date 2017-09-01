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
import models.registrationprogress.{Completed, NotStarted, Section, Started}
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
import generators.businesscustomer.ReviewDetailsGenerator
import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Matchers.{eq => eqTo, _}


class RegistrationProgressControllerSpec extends GenericTestHelper with MustMatchers with MockitoSugar with ReviewDetailsGenerator {

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

    val completeBusinessMatching = mock[BusinessMatching]
    when(completeBusinessMatching.isComplete) thenReturn true
    when(completeBusinessMatching.reviewDetails) thenReturn Some(reviewDetailsGen.sample.get)

    when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(completeBusinessMatching))
  }


  "RegistrationProgressController" when {
    "get is called" when {
      "the user is enrolled into the AMLS Account" must {
        "show the update your information page" in new Fixture {
          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(Seq.empty[Section])

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

            when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq.empty[Section])

            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(None))

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
            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some("AMLSREFNO")))

            when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq(
                Section("TESTSECTION1", Completed, false, mock[Call]),
                Section("TESTSECTION2", Completed, true, mock[Call])
              ))

            val responseF = controller.get()(request)
            status(responseF) must be(OK)
            val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
            submitButtons.size() must be(1)
            submitButtons.first().hasAttr("disabled") must be(false)
          }
        }


        "no section has changed" must {
          "disable the submission button" in new Fixture {
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
            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some("AMLSREFNO")))

            when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq(
                Section("TESTSECTION1", NotStarted, false, mock[Call]),
                Section("TESTSECTION2", Completed, true, mock[Call])
              ))

            val responseF = controller.get()(request)
            status(responseF) must be(OK)
            val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
            submitButtons.size() must be(1)
            submitButtons.first().hasAttr("disabled") must be(true)
          }
        }

        "no section has changed" must {
          "disable the submission button" in new Fixture {
            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some("AMLSREFNO")))

            when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(Seq(
                Section("TESTSECTION1", NotStarted, false, mock[Call]),
                Section("TESTSECTION2", Completed, false, mock[Call])
              ))

            val responseF = controller.get()(request)
            status(responseF) must be(OK)
            val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
            submitButtons.size() must be(1)
            submitButtons.first().hasAttr("disabled") must be(true)
          }
        }
      }

      "in any status" must {
        "hide the business matching section" in new Fixture {
          Seq(SubmissionReady, SubmissionReadyForReview, SubmissionDecisionApproved).foreach { subStatus =>
            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some("AMLSREFNO")))

            when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.statusService.getStatus(any(), any(), any()))
              .thenReturn(Future.successful(subStatus))

            val sections = Seq(
              Section(BusinessMatching.messageKey, Completed, false, mock[Call]),
              Section("TESTSECTION2", Completed, false, mock[Call])
            )

            when(controller.progressService.sections(mockCacheMap))
              .thenReturn(sections)

            val responseF = controller.get()(request)
            status(responseF) must be(OK)

            if (subStatus == SubmissionDecisionApproved) {
              val doc = Jsoup.parse(contentAsString((responseF)))
              doc.getElementsMatchingOwnText(Messages("amendment.text.1")).hasText must be(true)

              val elements = doc.getElementsMatchingOwnText(Messages("progress.visuallyhidden.view.amend"))
              elements.size() must be(sections.size - 1)
            }
          }
        }
      }

      "in the approved status" must {
        "show the correct text on the screen" in new Fixture {
          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some("AMLSREFNO")))

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val sections = Seq(
            Section(BusinessMatching.messageKey, Completed, false, mock[Call]),
            Section("TESTSECTION2", Completed, false, mock[Call])
          )

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(sections)

          val responseF = controller.get()(request)
          status(responseF) must be(OK)

          val doc = Jsoup.parse(contentAsString((responseF)))
          doc.getElementsMatchingOwnText(Messages("amendment.text.1")).hasText must be(true)

          val elements = doc.getElementsMatchingOwnText(Messages("progress.visuallyhidden.view.amend"))
          elements.size() must be(sections.size - 1)
        }
      }

      "the user is not enrolled into the AMLS Account" must {
        "show the registration progress page" in new Fixture {
          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(Seq.empty[Section])

          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(None))

          val responseF = controller.get()(request)
          status(responseF) must be(OK)
          val pageTitle = Messages("progress.title") + " - " +
            Messages("title.yapp") + " - " +
            Messages("title.amls") + " - " + Messages("title.gov")
          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
        }
      }

      "pre application must redirect to the landing controller" when {
        "the business matching is incomplete" in new Fixture {
          val businessMatching = mock[BusinessMatching]

          when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(businessMatching.isComplete) thenReturn false
          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(businessMatching))

          val completeSection = Section(BusinessMatching.messageKey, Started, true, controllers.routes.LandingController.get())
          when(controller.progressService.sections(mockCacheMap)) thenReturn Seq(completeSection)

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.LandingController.get().url)
        }
      }
    }
  }
}
