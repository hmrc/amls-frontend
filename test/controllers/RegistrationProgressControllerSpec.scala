/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import generators.AmlsReferenceNumberGenerator
import generators.businesscustomer.ReviewDetailsGenerator
import models.Country
import models.businessmatching._
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal.{AMLSTurnover, AMPTurnover, BusinessTurnover, CETransactionsInLast12Months, CashPayments, CashPaymentsCustomerNotMet, CustomersOutsideIsUK, CustomersOutsideUK, HowCashPaymentsReceived, InvolvedInOtherNo, InvolvedInOtherYes, MoneySources, MostTransactions, PaymentMethods, PercentageOfCashPaymentOver15000, Renewal, SendTheLargestAmountsOfMoney, TotalThroughput, TransactionsInLast12Months, WhichCurrencies}
import models.responsiblepeople.{ResponsiblePeopleValues, ResponsiblePerson}
import models.status._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.{AuthEnrolmentsService, ProgressService, RenewalService, SectionsProvider}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.registrationamendment.registration_amendment
import views.html.registrationprogress.registration_progress


import scala.concurrent.Future

class RegistrationProgressControllerSpec extends AmlsSpec
  with ReviewDetailsGenerator
  with AmlsReferenceNumberGenerator
  with ResponsiblePeopleValues {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val mockBusinessMatching = mock[BusinessMatching]
    val mockBusinessMatchingService = mock[BusinessMatchingService]
    lazy val view1 = app.injector.instanceOf[registration_progress]
    lazy val view2 = app.injector.instanceOf[registration_amendment]

    val controller = new RegistrationProgressController(
      SuccessfulAuthAction,
      progressService = mock[ProgressService],
      dataCache = mockCacheConnector,
      enrolmentsService = mock[AuthEnrolmentsService],
      statusService = mockStatusService,
      sectionsProvider = mock[SectionsProvider],
      businessMatchingService = mockBusinessMatchingService,
      serviceFlow = mockServiceFlow,
      renewalService = mock[RenewalService],
      ds = commonDependencies,
      cc = mockMcc,
      registration_progress = view1,
      registration_amendment = view2)

    mockApplicationStatus(SubmissionReady)
    mockCacheFetch[Renewal](None)

    when(mockBusinessMatching.isComplete) thenReturn true
    when(mockBusinessMatching.reviewDetails) thenReturn Some(reviewDetailsGen.sample.get)
    when(mockBusinessMatchingService.getAdditionalBusinessActivities(any[String]())(any(), any())) thenReturn OptionT.none[Future, Set[BusinessActivity]]

    when {
      controller.sectionsProvider.sectionsFromBusinessActivities(any(), any())(any())
    } thenReturn Seq.empty[Section]

    when {
      mockBusinessMatching.activities
    } thenReturn Some(BusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)))

    when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(mockBusinessMatching))
  }

  "RegistrationProgressController" when {
    "get is called" when {
      "the user is enrolled into the AMLS Account" must {
        "show the update your information page" in new Fixture {

          mockApplicationStatus(SubmissionReadyForReview)

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          when(controller.sectionsProvider.sections(mockCacheMap))
            .thenReturn(Seq.empty[Section])

          val responseF = controller.get()(request)
          status(responseF) must be(OK)

          val pageTitle = Messages("amendment.title") + " - " +
            Messages("title.amls") + " - " + Messages("title.gov")

          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
        }
      }

      "status is ReadyForRenewal and renewal data exists in mongoCache" must {
        "redirect to renewal registration progress" in new Fixture {

          mockCacheFetch[Renewal](Some(Renewal(Some(InvolvedInOtherNo))))

          mockApplicationStatus(ReadyForRenewal(None))

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val responseF = controller.get()(request)
          status(responseF) must be(SEE_OTHER)
          redirectLocation(responseF) must be(Some(renewal.routes.RenewalProgressController.get.url))
        }
      }
      "status is renewal submitted and renewal data exists in mongoCache" must {
        "show the registration amendment page" in new Fixture {

          when(controller.sectionsProvider.sections(mockCacheMap))
            .thenReturn(Seq(
              Section("TESTSECTION1", Completed, false, mock[Call]),
              Section("TESTSECTION2", Completed, true, mock[Call])
            ))

          mockCacheFetch[Renewal](Some(Renewal(Some(InvolvedInOtherNo))))

          mockApplicationStatus(RenewalSubmitted(None))

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val responseF = controller.get()(request)
          status(responseF) must be(OK)
          val pageTitle = Messages("amendment.title") + " - " +
            Messages("title.amls") + " - " + Messages("title.gov")

          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
        }
      }


      "redirect to registration progress" when {
        "status is ready for renewal and" must {
          "redirectWithNominatedOfficer" in new Fixture {

            mockApplicationStatus(ReadyForRenewal(None))

            mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

            when(controller.sectionsProvider.sections(mockCacheMap))
              .thenReturn(Seq.empty[Section])

            val responseF = controller.get()(request)
            status(responseF) must be(OK)

            val pageTitle = Messages("progress.title") + " - " +
              Messages("title.amls") + " - " + Messages("title.gov")
            Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle

          }
        }
      }

      "all sections are complete and" when {

        "a section has changed" when {

          "application is pre-submission" must {
            "enable the submission button" in new Fixture {

              when(controller.sectionsProvider.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", Completed, false, mock[Call]),
                  Section("TESTSECTION2", Completed, true, mock[Call])
                ))

              mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
              submitButtons.size() must be(1)
              submitButtons.first().hasAttr("disabled") must be(false)
            }
          }

          "application is post-submission" must {
            "show Submit Updates form" in new Fixture {

              mockApplicationStatus(SubmissionReadyForReview)

              mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

              when(controller.sectionsProvider.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", Completed, false, mock[Call]),
                  Section("TESTSECTION2", Completed, true, mock[Call])
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              contentAsString(responseF)  must include(Messages("progress.submit.updates"))

              val submitForm = Jsoup.parse(contentAsString(responseF)).select(".submit-application form")
              submitForm.attr("action") must be(controllers.routes.RegistrationProgressController.post().url)
              submitForm.select("button").text() must be(Messages("button.continue"))
            }
          }

        }

        "no section has changed" when {

          "application is pre-submission" must {
            "enable the submission button" in new Fixture {

              when(controller.sectionsProvider.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", Completed, false, mock[Call]),
                  Section("TESTSECTION2", Completed, false, mock[Call])
                ))

              mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
              submitButtons.size() must be(1)
              submitButtons.first().hasAttr("disabled") must be(false)
            }
          }

          "application is post-submission" must {
            "show View Status button" in new Fixture {

              mockApplicationStatus(SubmissionReadyForReview)

              mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

              when(controller.sectionsProvider.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", Completed, false, mock[Call]),
                  Section("TESTSECTION2", Completed, false, mock[Call])
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitDiv = Jsoup.parse(contentAsString(responseF)).select(".submit-application")
              val submitAnchor = submitDiv.select("#progress-continue")

              submitDiv.text() must include(Messages("progress.view.status"))
              submitAnchor.attr("href") must be(controllers.routes.StatusController.get().url)
              submitAnchor.text() must include("Check your status and messages")
            }
          }
        }
      }

      "some sections are not complete and" when {
        "a section has changed" when {

          "application is pre-submission" must {
            "disable the submission button" in new Fixture {

              when(controller.sectionsProvider.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", NotStarted, false, mock[Call]),
                  Section("TESTSECTION2", Completed, true, mock[Call])
                ))

              mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
              submitButtons.size() must be(0)
            }
          }

          "application is post-submission" must {
            "show View Status button" in new Fixture {

              mockApplicationStatus(SubmissionReadyForReview)

              mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

              when(controller.sectionsProvider.sections(mockCacheMap))
                .thenReturn(Seq(
                  Section("TESTSECTION1", NotStarted, false, mock[Call]),
                  Section("TESTSECTION2", Completed, true, mock[Call])
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val submitDiv = Jsoup.parse(contentAsString(responseF)).select(".submit-application")
              val submitAnchor = submitDiv.select("#progress-continue")

              submitDiv.text() must include(Messages("progress.view.status"))
              submitAnchor.attr("href") must be(controllers.routes.StatusController.get().url)
              submitAnchor.text() must include("Check your status and messages")
            }
          }

        }

        "no section has changed" must {
          "disable the submission button" in new Fixture {

            when(controller.sectionsProvider.sections(mockCacheMap))
              .thenReturn(Seq(
                Section("TESTSECTION1", NotStarted, false, mock[Call]),
                Section("TESTSECTION2", Completed, false, mock[Call])
              ))

            mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

            val responseF = controller.get()(request)
            status(responseF) must be(OK)

            val submitButtons = Jsoup.parse(contentAsString(responseF)).select("button[type=\"submit\"]")
            submitButtons.size() must be(0)
          }
        }
      }

      "in any status" must {
        "show the business activities and hide the business matching section" in new Fixture {
          Seq(SubmissionReady, SubmissionReadyForReview, SubmissionDecisionApproved).foreach { subStatus =>

            mockApplicationStatus(subStatus)

            mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

            val sections = Seq(
              Section(BusinessMatching.messageKey, Completed, false, mock[Call]),
              Section("TESTSECTION2", Completed, false, mock[Call])
            )

            when(controller.sectionsProvider.sections(mockCacheMap))
              .thenReturn(sections)

            val responseF = controller.get()(request)
            status(responseF) must be(OK)

            contentAsString(responseF) must not include Messages(s"progress.${BusinessMatching.messageKey}.name")

            Seq(
              "businessmatching.registerservices.servicename.lbl.01",
              "businessmatching.registerservices.servicename.lbl.03",
              "businessmatching.registerservices.servicename.lbl.04"
            ) foreach { msg =>
              contentAsString(responseF) must include(Messages(msg))
            }
          }
        }
      }

      "in the approved status" must {
        "show the correct text on the screen" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val sections = Seq(
            Section(BusinessMatching.messageKey, Completed, false, mock[Call]),
            Section("TESTSECTION2", Completed, false, mock[Call])
          )

          when(controller.sectionsProvider.sections(mockCacheMap))
            .thenReturn(sections)

          val responseF = controller.get()(request)
          status(responseF) must be(OK)

          val doc = Jsoup.parse(contentAsString((responseF)))
          doc.getElementsMatchingOwnText(Messages("amendment.text.1")).hasText must be(true)

          val elements = doc.getElementsMatchingOwnText(Messages("progress.visuallyhidden.view.amend"))
          elements.size() must be(sections.size - 1)

          doc.select("a.edit-preapp").text must include(Messages("progress.preapplication.canedit"))
        }
      }

      "the user is not enrolled into the AMLS Account" must {
        "show the registration progress page" in new Fixture {
          when(controller.sectionsProvider.sections(mockCacheMap))
            .thenReturn(Seq.empty[Section])

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val responseF = controller.get()(request)
          status(responseF) must be(OK)

          val pageTitle = Messages("progress.title") + " - " +
            Messages("title.amls") + " - " + Messages("title.gov")
          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
        }
      }

      "pre application must redirect to the landing controller" when {
        "the business matching is incomplete and status is pre-application" in new Fixture {
          when(mockBusinessMatching.isComplete) thenReturn false
          mockCacheFetch(Some(mockBusinessMatching))

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val completeSection = Section(BusinessMatching.messageKey, Started, true, controllers.routes.LandingController.get)
          when(controller.sectionsProvider.sections(mockCacheMap)) thenReturn Seq(completeSection)

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.LandingController.get.url)
        }
      }

      "pre-application must return 200 OK" when {
        "business matching is incomplete and status is not pre-application" in new Fixture {
          when(mockBusinessMatching.isComplete) thenReturn false
          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(mockBusinessMatching))

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val completeSection = Section(BusinessMatching.messageKey, Started, true, controllers.routes.LandingController.get)
          when(controller.sectionsProvider.sections(mockCacheMap)) thenReturn Seq(completeSection)

          val result = controller.get()(request)
          status(result) mustBe OK
        }
      }

      "new sections have been added" must {
        "show the new sections on the page" in new Fixture {
          when(mockBusinessMatching.isComplete) thenReturn true
          when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(mockBusinessMatching))

          val hvd = mock[models.hvd.Hvd]
          when(hvd.isComplete) thenReturn true

          val msb = mock[models.moneyservicebusiness.MoneyServiceBusiness]
          when(msb.isComplete(any(), any(), any())) thenReturn true

          mockCacheGetEntry(Some(msb), models.moneyservicebusiness.MoneyServiceBusiness.key)
          mockCacheGetEntry(Some(hvd), models.hvd.Hvd.key)

          val sections = Seq(models.moneyservicebusiness.MoneyServiceBusiness.section)

          when {
            controller.sectionsProvider.sections(any[CacheMap])
          } thenReturn sections

          val newSections = Seq(
            models.moneyservicebusiness.MoneyServiceBusiness.section,
            models.hvd.Hvd.section
          )

          when {
            controller.sectionsProvider.sectionsFromBusinessActivities(any(), any())(any())
          } thenReturn newSections

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val result = controller.get()(request)
          status(result) mustBe OK

          val html = Jsoup.parse(contentAsString(result))

          html.select("#existing-sections-list").text() must include(Messages("progress.hvd.name"))
        }
      }
    }

    "post is called" must {
      val completeRenewal = Renewal(
        Some(InvolvedInOtherYes("test")),
        Some(BusinessTurnover.First),
        Some(AMLSTurnover.First),
        Some(AMPTurnover.First),
        Some(CustomersOutsideIsUK(true)),
        Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
        Some(PercentageOfCashPaymentOver15000.First),
        Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other")))))),
        Some(TotalThroughput("01")),
        Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
        Some(TransactionsInLast12Months("1500")),
        Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
        Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
        Some(CETransactionsInLast12Months("123")),
        hasChanged = true
      )

      "when not in a position to renew" must {
        "redirect to the url provided by progressService" in new Fixture {
          val call = controllers.routes.RegistrationProgressController.get

          when {
            controller.statusService.getStatus(any(), any(), any())(any(), any())
          } thenReturn Future.successful(RenewalSubmitted(None))

          when(controller.renewalService.isRenewalComplete(any(), any())(any(), any()))
            .thenReturn(Future.successful(true))

          when(controller.renewalService.getRenewal(any())(any(), any()))
            .thenReturn(Future.successful(Some(completeRenewal)))

          when {
            controller.progressService.getSubmitRedirect(any[Option[String]](), any(), any())(any(), any())
          } thenReturn Future.successful(Some(call))

          val result = controller.post()(request)

          redirectLocation(result) must be(Some(call.url))
        }
      }

      "when in a position to renew" must {
        val inCompleteRenewal = completeRenewal.copy(
          businessTurnover = None
        )

        "redirect to the renew registration controller" in new Fixture {
          when {
            controller.statusService.getStatus(any(), any(), any())(any(), any())
          } thenReturn Future.successful(ReadyForRenewal(Some(new LocalDate())))

          when(controller.renewalService.isRenewalComplete(any(), any())(any(), any()))
            .thenReturn(Future.successful(false))

          when(controller.renewalService.getRenewal(any())(any(), any()))
            .thenReturn(Future.successful(Some(inCompleteRenewal)))

          when(controller.renewalService.getRenewal(any())(any(), any()))
            .thenReturn(Future.successful(Some(inCompleteRenewal)))

          val result = controller.post()(request)

          redirectLocation(result) must be(Some(controllers.declaration.routes.RenewRegistrationController.get.url))
        }
      }

      "return INTERNAL_SERVER_ERROR if no call is returned" in new Fixture {
        when {
          controller.statusService.getStatus(any(), any(), any())(any(), any())
        } thenReturn Future.successful(RenewalSubmitted(None))

        when(controller.renewalService.isRenewalComplete(any(), any())(any(), any()))
          .thenReturn(Future.successful(true))

        when(controller.renewalService.getRenewal(any())(any(), any()))
          .thenReturn(Future.successful(Some(completeRenewal)))

        when {
          controller.progressService.getSubmitRedirect(any[Option[String]](), any(), any())(any(), any())
        } thenReturn Future.successful(None)

        val result = controller.post()(request)

        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }
  }

}
