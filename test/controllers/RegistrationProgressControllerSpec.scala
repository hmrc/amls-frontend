/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.AmlsConnector
import controllers.actions.SuccessfulAuthAction
import generators.AmlsReferenceNumberGenerator
import generators.businesscustomer.ReviewDetailsGenerator
import models.Country
import models.businessmatching.BusinessActivity.{AccountancyServices, BillPaymentServices, EstateAgentBusinessService}
import models.businessmatching._
import models.registrationprogress._
import models.renewal.{Renewal, _}
import models.responsiblepeople.{ResponsiblePeopleValues, ResponsiblePerson}
import models.status._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import play.api.http.Status.OK
import play.api.test.Helpers._
import play.api.test.Injecting
import services.businessmatching.BusinessMatchingService
import services.{AuthEnrolmentsService, ProgressService, RenewalService, SectionsProvider}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.registrationamendment.RegistrationAmendmentView
import views.html.registrationprogress.RegistrationProgressView

import java.time.LocalDate
import scala.concurrent.Future

class RegistrationProgressControllerSpec extends AmlsSpec
  with ReviewDetailsGenerator
  with AmlsReferenceNumberGenerator
  with ResponsiblePeopleValues
  with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val mockBusinessMatching = mock[BusinessMatching]
    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockSectionsProvider = mock[SectionsProvider]
    lazy val view1 = inject[RegistrationProgressView]
    lazy val view2 = inject[RegistrationAmendmentView]

    val renewalService: RenewalService = mock[RenewalService]
    val controller = new RegistrationProgressController(
      SuccessfulAuthAction,
      progressService = mock[ProgressService],
      dataCache = mockCacheConnector,
      enrolmentsService = mock[AuthEnrolmentsService],
      statusService = mockStatusService,
      sectionsProvider = mockSectionsProvider,
      businessMatchingService = mockBusinessMatchingService,
      serviceFlow = mockServiceFlow,
      amlsConnector = mock[AmlsConnector],
      renewalService = mock[RenewalService],
      ds = commonDependencies,
      cc = mockMcc,
      registration_progress = view1,
      registration_amendment = view2)

    mockApplicationStatus(SubmissionReady)
    mockCacheFetch[Renewal](None)
    when(renewalService.isRenewalFlow(any(), any(), any())(any(), any())).thenReturn(Future.successful(false))

    when(mockBusinessMatching.isComplete) thenReturn true
    when(mockBusinessMatching.reviewDetails) thenReturn Some(reviewDetailsGen.sample.get)
    when(mockBusinessMatchingService.getAdditionalBusinessActivities(any[String]())(any())) thenReturn OptionT.none[Future, Set[BusinessActivity]]

    when {
      mockSectionsProvider.taskRowsFromBusinessActivities(any(), any())(any(), any())
    } thenReturn Seq.empty[TaskRow]

    when(mockSectionsProvider.taskRows(any[Cache])(any()))
      .thenReturn(Seq.empty[TaskRow])

    when {
      mockBusinessMatching.activities
    } thenReturn Some(BusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)))

    when(mockCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(mockBusinessMatching))

    when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful((NotCompleted, None)))

  }

  "RegistrationProgressController" when {
    "get is called" when {
      "the user is enrolled into the AMLS Account" must {
        "show the update your information page" in new Fixture {

          mockApplicationStatus(SubmissionReadyForReview)

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          when(mockSectionsProvider.taskRows(meq(mockCacheMap))(any()))
            .thenReturn(Seq.empty[TaskRow])

          val responseF = controller.get()(request)
          status(responseF) must be(OK)

          val pageTitle = messages("amendment.title") + " - " +
            messages("title.amls") + " - " + messages("title.gov")

          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
        }
      }

      "status is ReadyForRenewal and renewal data exists in mongoCache" must {
        "redirect to renewal registration progress" in new Fixture {

          mockCacheFetch[Renewal](Some(Renewal(Some(InvolvedInOtherNo))))
          when(renewalService.isRenewalFlow(any(), any(), any())(any(), any())).thenReturn(Future.successful(true))

          mockApplicationStatus(ReadyForRenewal(None))

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val responseF = controller.get()(request)
          status(responseF) must be(SEE_OTHER)
          redirectLocation(responseF) must be(Some(renewal.routes.RenewalProgressController.get.url))
        }
      }
      "status is renewal submitted and renewal data exists in mongoCache" must {
        "show the registration amendment page" in new Fixture {

          when(mockSectionsProvider.taskRows(any[Cache])(any()))
            .thenReturn(Seq(
              TaskRow("TESTSECTION1", "/foo", false, Completed, TaskRow.completedTag),
              TaskRow("TESTSECTION2", "/bar", true, Completed, TaskRow.completedTag)
            ))

          mockApplicationStatus(RenewalSubmitted(None))

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val responseF = controller.get()(request)
          status(responseF) must be(OK)
          val pageTitle = messages("amendment.title") + " - " +
            messages("title.amls") + " - " + messages("title.gov")

          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
        }
      }


      "render the registration progress" when {
        "status is ready for renewal and" must {
          "redirectWithNominatedOfficer" in new Fixture {

            mockApplicationStatus(ReadyForRenewal(None))

            mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

            when(mockSectionsProvider.taskRows(any[Cache])(any()))
              .thenReturn(Seq.empty[TaskRow])

            when(controller.sectionsProvider.taskRows(meq(mockCacheMap))(any()))
              .thenReturn(Seq.empty[TaskRow])

            val responseF = controller.get()(request)
            status(responseF) must be(OK)

            val pageTitle = messages("progress.title") + " - " +
              messages("title.amls") + " - " + messages("title.gov")
            Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle

          }
        }
      }

      "all sections are complete and" when {

        "a section has changed" when {

          "application is pre-submission" must {
            "enable the submission button" when {
              "all tasks have the status 'Completed'" in new Fixture {

                when(mockSectionsProvider.taskRows(any[Cache])(any()))
                  .thenReturn(Seq(
                    TaskRow("TESTSECTION1", "/foo", false, Completed, TaskRow.completedTag),
                    TaskRow("TESTSECTION2", "/bar", true, Completed, TaskRow.completedTag)
                  ))

                mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

                val responseF = controller.get()(request)
                status(responseF) must be(OK)

                val submitButton = Jsoup.parse(contentAsString(responseF)).getElementById("progress-continue")
                submitButton.hasAttr("disabled") must be(false)
              }

              "all tasks have the status 'Updated'" in new Fixture {

                when(mockSectionsProvider.taskRows(any[Cache])(any()))
                  .thenReturn(Seq(
                    TaskRow("TESTSECTION1", "/foo", false, Updated, TaskRow.completedTag),
                    TaskRow("TESTSECTION2", "/bar", true, Updated, TaskRow.completedTag)
                  ))

                mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

                val responseF = controller.get()(request)
                status(responseF) must be(OK)

                val submitButton = Jsoup.parse(contentAsString(responseF)).getElementById("progress-continue")
                submitButton.hasAttr("disabled") must be(false)
              }

              "all tasks have the status 'Completed' or 'Updated'" in new Fixture {

                when(mockSectionsProvider.taskRows(any[Cache])(any()))
                  .thenReturn(Seq(
                    TaskRow("TESTSECTION1", "/foo", false, Completed, TaskRow.completedTag),
                    TaskRow("TESTSECTION2", "/bar", true, Updated, TaskRow.completedTag)
                  ))

                mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

                val responseF = controller.get()(request)
                status(responseF) must be(OK)

                val submitButton = Jsoup.parse(contentAsString(responseF)).getElementById("progress-continue")
                submitButton.hasAttr("disabled") must be(false)
              }
            }
          }

          "application is post-submission" must {
            "show Submit Updates form" when {
              "all tasks have the status 'Completed'" in new Fixture {

                mockApplicationStatus(SubmissionReadyForReview)

                mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

                when(mockSectionsProvider.taskRows(any[Cache])(any()))
                  .thenReturn(Seq(
                    TaskRow("TESTSECTION1", "/foo", false, Completed, TaskRow.completedTag),
                    TaskRow("TESTSECTION2", "/bar", true, Completed, TaskRow.completedTag)
                  ))

                val responseF = controller.get()(request)
                status(responseF) must be(OK)

                val html = Jsoup.parse(contentAsString(responseF))
                html.text() must include(messages("progress.submit.updates"))

                html.getElementsByTag("form")
                  .first().attr("action") must be(controllers.routes.RegistrationProgressController.post().url)

                html.getElementById("progress-continue").select("button").text() must be(messages("button.continue"))
              }

              "all tasks have the status 'Updated'" in new Fixture {

                mockApplicationStatus(SubmissionReadyForReview)

                mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

                when(mockSectionsProvider.taskRows(any[Cache])(any()))
                  .thenReturn(Seq(
                    TaskRow("TESTSECTION1", "/foo", true, Updated, TaskRow.updatedTag),
                    TaskRow("TESTSECTION2", "/bar", true, Updated, TaskRow.updatedTag)
                  ))

                val responseF = controller.get()(request)
                status(responseF) must be(OK)

                val html = Jsoup.parse(contentAsString(responseF))
                html.text() must include(messages("progress.submit.updates"))

                html.getElementsByTag("form")
                  .first().attr("action") must be(controllers.routes.RegistrationProgressController.post().url)

                html.getElementById("progress-continue").select("button").text() must be(messages("button.continue"))
              }

              "all tasks have the status 'Completed' or 'Updated'" in new Fixture {

                mockApplicationStatus(SubmissionReadyForReview)

                mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

                when(mockSectionsProvider.taskRows(any[Cache])(any()))
                  .thenReturn(Seq(
                    TaskRow("TESTSECTION1", "/foo", false, Completed, TaskRow.completedTag),
                    TaskRow("TESTSECTION2", "/bar", true, Updated, TaskRow.updatedTag)
                  ))

                val responseF = controller.get()(request)
                status(responseF) must be(OK)

                val html = Jsoup.parse(contentAsString(responseF))
                html.text() must include(messages("progress.submit.updates"))

                html.getElementsByTag("form")
                  .first().attr("action") must be(controllers.routes.RegistrationProgressController.post().url)

                html.getElementById("progress-continue").select("button").text() must be(messages("button.continue"))
              }
            }
          }
        }

        "no section has changed" when {

          "application is pre-submission" must {
            "enable the submission button" when {
              "all tasks have the status 'Completed'" in new Fixture {

                when(mockSectionsProvider.taskRows(any[Cache])(any()))
                  .thenReturn(Seq(
                    TaskRow("TESTSECTION1", "/foo", false, Completed, TaskRow.completedTag),
                    TaskRow("TESTSECTION2", "/bar", false, Completed, TaskRow.completedTag)
                  ))

                mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

                val responseF = controller.get()(request)
                status(responseF) must be(OK)
                val submitButton = Jsoup.parse(contentAsString(responseF)).getElementById("progress-continue")
                submitButton.hasAttr("disabled") must be(false)
              }
            }
          }

          "application is post-submission" must {
            "show View Status button" in new Fixture {

              mockApplicationStatus(SubmissionReadyForReview)

              mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

              when(mockSectionsProvider.taskRows(any[Cache])(any()))
                .thenReturn(Seq(
                  TaskRow("TESTSECTION1", "/foo", false, Completed, TaskRow.completedTag),
                  TaskRow("TESTSECTION2", "/bar", false, Completed, TaskRow.completedTag)
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val statusLink = Jsoup.parse(contentAsString(responseF)).getElementById("progress-continue")

              statusLink.parent().siblingElements().text() must include(messages("progress.view.status"))
              statusLink.attr("href") must be(controllers.routes.StatusController.get().url)
              statusLink.text() must include("Check your status and messages")
            }
          }
        }
      }

      "some sections are not complete and" when {
        "a section has changed" when {

          "application is pre-submission" must {
            "disable the submission button" in new Fixture {

              when(mockSectionsProvider.taskRows(any[Cache])(any()))
                .thenReturn(Seq(
                  TaskRow("TESTSECTION1", "/foo", false, NotStarted, TaskRow.notStartedTag),
                  TaskRow("TESTSECTION2", "/bar", true, Completed, TaskRow.completedTag)
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

              when(mockSectionsProvider.taskRows(any[Cache])(any()))
                .thenReturn(Seq(
                  TaskRow("TESTSECTION1", "/foo", false, NotStarted, TaskRow.notStartedTag),
                  TaskRow("TESTSECTION2", "/bar", true, Completed, TaskRow.completedTag)
                ))

              val responseF = controller.get()(request)
              status(responseF) must be(OK)

              val statusLink = Jsoup.parse(contentAsString(responseF)).getElementById("progress-continue")

              statusLink.parent().siblingElements().text() must include(messages("progress.view.status"))
              statusLink.attr("href") must be(controllers.routes.StatusController.get().url)
              statusLink.text() must include("Check your status and messages")
            }
          }

        }

        "no section has changed" must {
          "disable the submission button" in new Fixture {

            when(mockSectionsProvider.taskRows(any[Cache])(any()))
              .thenReturn(Seq(
                TaskRow("TESTSECTION1", "/foo", false, NotStarted, TaskRow.notStartedTag),
                TaskRow("TESTSECTION2", "/bar", false, Completed, TaskRow.completedTag)
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

            when(mockSectionsProvider.taskRows(any[Cache])(any()))
              .thenReturn(
                Seq(
                  TaskRow(BusinessMatching.messageKey, "/foo", false, Completed, TaskRow.completedTag),
                  TaskRow("TESTSECTION2", "/bar", false, Completed, TaskRow.completedTag)
                )
              )

            val responseF = controller.get()(request)
            status(responseF) must be(OK)

            contentAsString(responseF) must not include messages(s"progress.${BusinessMatching.messageKey}.name")

            Seq(
              "businessmatching.registerservices.servicename.lbl.01",
              "businessmatching.registerservices.servicename.lbl.03",
              "businessmatching.registerservices.servicename.lbl.04"
            ) foreach { msg =>
              contentAsString(responseF) must include(messages(msg))
            }
          }
        }
      }

      "in the approved status" must {
        "show the correct text on the screen" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val sections = Seq(
            TaskRow(BusinessMatching.messageKey, "/foo", false, Completed, TaskRow.completedTag),
            TaskRow("TESTSECTION2", "/bar", false, Completed, TaskRow.completedTag)
          )

          when(mockSectionsProvider.taskRows(any[Cache])(any()))
            .thenReturn(sections)

          val responseF = controller.get()(request)
          status(responseF) must be(OK)

          val doc = Jsoup.parse(contentAsString((responseF)))
          doc.getElementsMatchingOwnText(messages("amendment.text.1")).hasText must be(true)

          val elements = doc.getElementsMatchingOwnText(messages("progress.visuallyhidden.view.amend"))
          elements.size() must be(sections.size - 1)

          doc.select("a.edit-preapp").text must include(messages("progress.preapplication.canedit"))
        }
      }

      "the user is not enrolled into the AMLS Account" must {
        "show the registration progress page" in new Fixture {
          when(mockSectionsProvider.taskRows(meq(mockCacheMap))(any()))
            .thenReturn(Seq.empty[TaskRow])

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val responseF = controller.get()(request)
          status(responseF) must be(OK)

          val pageTitle = messages("progress.title") + " - " +
            messages("title.amls") + " - " + messages("title.gov")
          Jsoup.parse(contentAsString(responseF)).title mustBe pageTitle
        }
      }

      "pre application must redirect to the landing controller" when {
        "the business matching is incomplete and status is pre-application" in new Fixture {
          when(mockBusinessMatching.isComplete) thenReturn false
          mockCacheFetch(Some(mockBusinessMatching))

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val completeTaskRow = TaskRow(BusinessMatching.messageKey, controllers.routes.LandingController.get().url, true, Started, TaskRow.incompleteTag)
          when(mockSectionsProvider.taskRows(meq(mockCacheMap))(any())) thenReturn Seq(completeTaskRow)

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

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val completeTaskRow = TaskRow(BusinessMatching.messageKey, controllers.routes.LandingController.get().url, true, Started, TaskRow.incompleteTag)
          when(mockSectionsProvider.taskRows(any[Cache])(any())) thenReturn Seq(completeTaskRow)

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

          val taskRow = Seq(models.moneyservicebusiness.MoneyServiceBusiness.taskRow)

          when(mockSectionsProvider.taskRows(any[Cache])(any()))
            .thenReturn(taskRow)

          val newTaskRows = Seq(
            models.moneyservicebusiness.MoneyServiceBusiness.taskRow,
            models.hvd.Hvd.taskRow
          )

          when {
            mockSectionsProvider.taskRowsFromBusinessActivities(any(), any())(any(), any())
          } thenReturn newTaskRows

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(completeResponsiblePerson)), ResponsiblePerson.key)

          val result = controller.get()(request)
          status(result) mustBe OK

          val html = Jsoup.parse(contentAsString(result))

          html.getElementsByClass("govuk-task-list__item").get(1).text() must include(messages("progress.hvd.name"))
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
          val call = controllers.routes.RegistrationProgressController.get()

          when {
            controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
          } thenReturn Future.successful(RenewalSubmitted(None))

          when(controller.renewalService.isRenewalComplete(any(), any())(any()))
            .thenReturn(Future.successful(true))

          when(controller.renewalService.getRenewal(any())).thenReturn(Future.successful(Some(completeRenewal)))

          when {
            controller.progressService.getSubmitRedirect(any[Option[String]](), any(), any())(any(), any(), any())
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
            controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
          } thenReturn Future.successful(ReadyForRenewal(Some(LocalDate.now())))

          when(controller.renewalService.isRenewalComplete(any(), any())(any()))
            .thenReturn(Future.successful(false))

          when(controller.renewalService.getRenewal(any())).thenReturn(Future.successful(Some(inCompleteRenewal)))

          when(controller.renewalService.getRenewal(any())).thenReturn(Future.successful(Some(inCompleteRenewal)))

          val result = controller.post()(request)

          redirectLocation(result) must be(Some(controllers.declaration.routes.RenewRegistrationController.get().url))
        }
      }

      "return INTERNAL_SERVER_ERROR if no call is returned" in new Fixture {
        when {
          controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
        } thenReturn Future.successful(RenewalSubmitted(None))

        when(controller.renewalService.isRenewalComplete(any(), any())(any()))
          .thenReturn(Future.successful(true))

        when(controller.renewalService.getRenewal(any())).thenReturn(Future.successful(Some(completeRenewal)))

        when {
          controller.progressService.getSubmitRedirect(any[Option[String]](), any(), any())(any(), any(), any())
        } thenReturn Future.successful(None)

        val result = controller.post()(request)

        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }
  }
}
