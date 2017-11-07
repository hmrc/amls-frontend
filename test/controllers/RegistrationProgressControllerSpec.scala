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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import generators.AmlsReferenceNumberGenerator
import generators.businesscustomer.ReviewDetailsGenerator
import models.businessmatching._
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal.{InvolvedInOtherNo, Renewal}
import models.status._
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.{AuthEnrolmentsService, ProgressService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationProgressControllerSpec extends GenericTestHelper
  with MustMatchers
  with MockitoSugar
  with ReviewDetailsGenerator
  with AmlsReferenceNumberGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val mockBusinessMatching = mock[BusinessMatching]
    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val controller = new RegistrationProgressController {
      override val authConnector = self.authConnector
      override protected[controllers] val progressService: ProgressService = mock[ProgressService]
      override protected[controllers] val dataCache: DataCacheConnector = mockCacheConnector
      override protected[controllers] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override protected[controllers] val statusService: StatusService = mockStatusService
      override protected[controllers] val businessMatchingService = mockBusinessMatchingService
      override protected[controllers] val serviceFlow = mockServiceFlow
    }

    when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionReady)
    when(controller.dataCache.fetch[Renewal](any())(any(), any(), any())) thenReturn Future.successful(None)
    when(mockBusinessMatching.isComplete) thenReturn true
    when(mockBusinessMatching.reviewDetails) thenReturn Some(reviewDetailsGen.sample.get)
    when(mockBusinessMatchingService.getAdditionalBusinessActivities(any(), any(), any())) thenReturn OptionT.none[Future, Set[BusinessActivity]]
    when(mockServiceFlow.setInServiceFlowFlag(eqTo(false))(any(), any(), any())) thenReturn Future.successful(mockCacheMap)

    when {
      controller.progressService.sectionsFromBusinessActivities(any(), any())(any())
    } thenReturn Set.empty[Section]

    when {
      mockBusinessMatching.activities
    } thenReturn Some(BusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)))

    when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(mockBusinessMatching))
  }

  "RegistrationProgressController" when {
    "get is called" when {
      "the user is enrolled into the AMLS Account" must {
        "show the update your information page" in new Fixture {
          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.progressService.sections(mockCacheMap))
            .thenReturn(Seq.empty[Section])

          val responseF = controller.get()(request)
          status(responseF) must be(OK)

          val pageTitle = Messages("amendment.title") + " - " +
            Messages("title.yapp") + " - " +
            Messages("title.amls") + " - " + Messages("title.gov")

          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle

          verify(mockServiceFlow).setInServiceFlowFlag(eqTo(false))(any(), any(), any())
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

        "a section has changed" when {

          "application is pre-submission" must {
            "enable the submission button" in new Fixture {
              when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
                .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

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

          "application is post-submission" must {
            "show Submit Updates form" in new Fixture {

              when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
                .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

              when(controller.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              when(controller.progressService.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", Completed, false, mock[Call]),
                  Section("TESTSECTION2", Completed, true, mock[Call])
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitForm = Jsoup.parse(contentAsString(responseF)).select(".submit-application form")
              submitForm.text() must include(Messages("progress.submit.updates"))
              submitForm.attr("action") must be(controllers.routes.RegistrationProgressController.post().url)
              submitForm.select("button").text() must be(Messages("button.continue"))
            }
          }

        }

        "no section has changed" when {

          "application is pre-submission" must {
            "enable the submission button" in new Fixture {

              when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
                .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

              when(controller.progressService.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", Completed, false, mock[Call]),
                  Section("TESTSECTION2", Completed, false, mock[Call])
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
              submitButtons.size() must be(1)
              submitButtons.first().hasAttr("disabled") must be(false)
            }
          }

          "application is post-submission" must {
            "show View Status button" in new Fixture {

              when(controller.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
                .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

              when(controller.progressService.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", Completed, false, mock[Call]),
                  Section("TESTSECTION2", Completed, false, mock[Call])
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitDiv = Jsoup.parse(contentAsString(responseF)).select(".submit-application")
              val submitAnchor = submitDiv.select("a")

              submitDiv.text() must include(Messages("progress.view.status"))
              submitAnchor.attr("href") must be(controllers.routes.StatusController.get().url)
              submitAnchor.text() must include(Messages("button.continue"))
            }
          }

        }

      }

      "some sections are not complete and" when {
        "a section has changed" when {

          "application is pre-submission" must {
            "disable the submission button" in new Fixture {
              when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
                .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

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

          "application is post-submission" must {
            "show View Status button" in new Fixture {

              when(controller.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
                .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

              when(controller.progressService.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", NotStarted, false, mock[Call]),
                  Section("TESTSECTION2", Completed, true, mock[Call])
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitDiv = Jsoup.parse(contentAsString(responseF)).select(".submit-application")
              val submitAnchor = submitDiv.select("a")

              submitDiv.text() must include(Messages("progress.view.status"))
              submitAnchor.attr("href") must be(controllers.routes.StatusController.get().url)
              submitAnchor.text() must include(Messages("button.continue"))
            }
          }

        }

        "no section has changed" must {
          "disable the submission button" in new Fixture {
            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

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
        "show the business activities and hide the business matching section" in new Fixture {
          Seq(SubmissionReady, SubmissionReadyForReview, SubmissionDecisionApproved).foreach { subStatus =>
            when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

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

            contentAsString(responseF) must not include Messages(s"progress.${BusinessMatching.messageKey}.name")

            Seq(
              "businessmatching.registerservices.servicename.lbl.01",
              "businessmatching.registerservices.servicename.lbl.02",
              "businessmatching.registerservices.servicename.lbl.03"
            ) foreach { msg =>
              contentAsString(responseF) must include(Messages(msg))
            }
          }
        }
      }

      "in the approved status" must {
        "show the correct text on the screen" in new Fixture {
          when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

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

          doc.select("a.edit-preapp").text must include(Messages("progress.preapplication.readonly"))
        }
      }

      "the user is not enrolled into the AMLS Account" must {
        "show the registration progress page" in new Fixture {
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
        "the business matching is incomplete and status is pre-application" in new Fixture {
          when(mockBusinessMatching.isComplete) thenReturn false
          mockCacheFetch(Some(mockBusinessMatching))

          val completeSection = Section(BusinessMatching.messageKey, Started, true, controllers.routes.LandingController.get())
          when(controller.progressService.sections(mockCacheMap)) thenReturn Seq(completeSection)

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.LandingController.get().url)
        }
      }

      "pre-application must return 200 OK" when {
        "business matching is incomplete and status is not pre-application" in new Fixture {
          when(mockBusinessMatching.isComplete) thenReturn false
          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(mockBusinessMatching))
          mockApplicationStatus(SubmissionDecisionApproved)

          when {
            controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext])
          } thenReturn Future.successful(Some(amlsRegistrationNumber))

          val completeSection = Section(BusinessMatching.messageKey, Started, true, controllers.routes.LandingController.get())
          when(controller.progressService.sections(mockCacheMap)) thenReturn Seq(completeSection)

          val result = controller.get()(request)
          status(result) mustBe OK
        }
      }

      "new sections have been added" must {
        "show the new sections on the page" in new Fixture {
          when(mockBusinessMatching.isComplete) thenReturn true
          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(mockBusinessMatching))

          when {
            controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext])
          } thenReturn Future.successful(Some(amlsRegistrationNumber))

          val hvd = mock[models.hvd.Hvd]
          when(hvd.isComplete) thenReturn true

          val msb = mock[models.moneyservicebusiness.MoneyServiceBusiness]
          when(msb.isComplete(any(), any())) thenReturn true

          mockCacheGetEntry(Some(msb), models.moneyservicebusiness.MoneyServiceBusiness.key)
          mockCacheGetEntry(Some(hvd), models.hvd.Hvd.key)

          val sections = Seq(models.moneyservicebusiness.MoneyServiceBusiness.section)

          when {
            controller.progressService.sections(any())
          } thenReturn sections

          val newSections = Set(
            models.moneyservicebusiness.MoneyServiceBusiness.section,
            models.hvd.Hvd.section
          )

          when {
            controller.progressService.sectionsFromBusinessActivities(any(), any())(any())
          } thenReturn newSections

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get()(request)
          status(result) mustBe OK

          val html = Jsoup.parse(contentAsString(result))

          html.select(".progress-new-sections").text() must include(Messages("progress.hvd.name"))
          html.select(".progress-existing-sections").text() must not include Messages("progress.hvd.name")
        }
      }
    }

    "post is called" must {
      "redirect to the url provided by progressService" in new Fixture {
        val call = controllers.routes.RegistrationProgressController.get()

        when {
          controller.progressService.getSubmitRedirect(any(), any(), any())
        } thenReturn Future.successful(Some(call))

        val result = controller.post()(request)

        redirectLocation(result) must be(Some(call.url))
      }

      "return INTERNAL_SERVER_ERROR if no call is returned" in new Fixture {

        when {
          controller.progressService.getSubmitRedirect(any(), any(), any())
        } thenReturn Future.successful(None)

        val result = controller.post()(request)

        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }
  }

}
