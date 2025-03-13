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

package controllers.responsiblepeople

import config.{AmlsErrorHandler, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import controllers.declaration
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.BusinessType.{LimitedCompany, Partnership}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.responsiblepeople.ResponsiblePerson.flowFromDeclaration
import models.responsiblepeople._
import models.status._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.Injecting
import services.StatusService
import services.businessmatching.RecoverActivitiesService
import services.cache.Cache
import utils.responsiblepeople.CheckYourAnswersHelper
import utils.{AmlsSpec, DependencyMocks}
import views.html.responsiblepeople.CheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.Future

class DetailedAnswersControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ResponsiblePeopleValues
    with BusinessMatchingGenerator
    with OptionValues
    with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[CheckYourAnswersView]
    val controller = new DetailedAnswersController(
      dataCacheConnector = mock[DataCacheConnector],
      recoverActivitiesService = mock[RecoverActivitiesService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mock[StatusService],
      config = mock[ApplicationConfig],
      cc = mockMcc,
      cyaHelper = inject[CheckYourAnswersHelper],
      view = view,
      error = errorView,
      amlsErrorHandler = inject[AmlsErrorHandler]
    )

    val businessMatching = BusinessMatching()

    val personName = PersonName("first name", None, "last name")

    def setupMocksFor(model: ResponsiblePerson, status: SubmissionStatus = SubmissionReady) = {

      when {
        controller.dataCacheConnector.fetchAll(any())
      } thenReturn Future.successful(Some(mockCacheMap))

      when {
        mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any())
      } thenReturn Some(businessMatching)

      when {
        mockCacheMap.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
      } thenReturn Some(Seq(model))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any()))
        .thenReturn(Future.successful(Some(businessMatching)))

      when(controller.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(status))
    }

  }

  "DetailedAnswersController" when {

    "get is called - from the yourResponsiblePeople controller" must {
      "respond with OK and show the detailed answers page with a 'confirm and continue'" in new Fixture {

        setupMocksFor(completeResponsiblePerson)

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(messages("title.cya"))
        contentAsString(result) must include("/anti-money-laundering/responsible-people/check-your-answers")
      }

      "redirect to itself after performing a successful recovery of missing business types" in new Fixture {
        override val businessMatching: BusinessMatching = BusinessMatching(activities = Some(BusinessActivities(Set())))

        setupMocksFor(completeResponsiblePerson)

        when(controller.recoverActivitiesService.recover(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val result: Future[Result] = controller.get(1)(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.DetailedAnswersController.get(1).url)
      }

      "return an internal server error after failing to recover missing business types" in new Fixture {
        override val businessMatching: BusinessMatching = BusinessMatching(activities = Some(BusinessActivities(Set())))

        setupMocksFor(completeResponsiblePerson)

        when(controller.recoverActivitiesService.recover(any())(any(), any(), any()))
          .thenReturn(Future.successful(false))

        val result: Future[Result] = controller.get(1)(request)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "load yourAnswers page when the status is approved" in new Fixture {

      val model = completeResponsiblePerson.copy(
        personName = Some(personName),
        legalName = Some(PreviousName(Some(false), None, None, None)),
        lineId = Some(121212)
      )
      setupMocksFor(model, SubmissionDecisionApproved)

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element  = document.getElementsMatchingOwnText(
        messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName)
      )
      element.hasAttr("href") must be(true)
    }

    "load yourAnswers page when the status is renewal submitted" in new Fixture {
      val model = completeResponsiblePerson.copy(
        personName = Some(personName),
        legalName = Some(PreviousName(Some(false), None, None, None)),
        lineId = Some(121212)
      )

      setupMocksFor(model, RenewalSubmitted(None))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element  = document.getElementsMatchingOwnText(
        messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName)
      )
      element.hasAttr("href") must be(true)
    }

    "load yourAnswers page when the status is approved and has no lineId" in new Fixture {

      val model = completeResponsiblePerson.copy(
        personName = Some(personName),
        legalName = Some(PreviousName(Some(false), None, None, None)),
        lineId = None
      )
      setupMocksFor(model, SubmissionDecisionApproved)

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element  = document.getElementsMatchingOwnText(
        messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName)
      )
      element.hasAttr("href") must be(false)
    }

    "load yourAnswers page when the status is ready for renewal" in new Fixture {

      val model = completeResponsiblePerson.copy(
        personName = Some(personName),
        legalName = Some(PreviousName(Some(false), None, None, None)),
        lineId = Some(121212)
      )
      setupMocksFor(model, ReadyForRenewal(None))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element  = document.getElementsMatchingOwnText(
        messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName)
      )
      element.hasAttr("href") must be(true)
    }

    "get is called - NOT from the yourAnswers controller" must {
      "respond with OK and show the detailed answers page with a 'confirm and continue'" in new Fixture {

        val model = completeResponsiblePerson.copy(
          personName = Some(personName),
          legalName = Some(PreviousName(Some(false), None, None, None)),
          lineId = Some(121212)
        )
        setupMocksFor(model)

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(messages("title.cya"))
        contentAsString(result) must not include "/anti-money-laundering/responsible-people/your-answers"
      }
    }

    "get is called from any location" when {
      "section data is available" must {
        "respond with OK and show the detailed answers page with the correct title" in new Fixture {

          val model = completeResponsiblePerson.copy(
            personName = Some(personName),
            legalName = Some(PreviousName(Some(false), None, None, None)),
            lineId = Some(121212)
          )
          setupMocksFor(model)

          val result = controller.get(1)(request)
          status(result) must be(OK)

          contentAsString(result) must include(messages("title.cya"))
        }

        "respond with OK and show the detailed answers page with a correctly formatted responsiblePerson startDate" in new Fixture {

          private val testStartDate = LocalDate.of(1999, 1, 1)

          val model = completeResponsiblePerson.copy(
            personName = Some(personName),
            legalName = Some(PreviousName(Some(false), None, None, None)),
            lineId = Some(121212),
            positions = Some(Positions(Set(BeneficialOwner), Some(PositionStartDate(testStartDate))))
          )

          setupMocksFor(model)

          val result = controller.get(1)(request)
          status(result) must be(OK)

          contentAsString(result) must include("1 January 1999")
        }

        "respond with 303 and redirect to the your responsible people page if for any reason rp model is not complete" in new Fixture {

          val model = completeResponsiblePerson.copy(
            personName = Some(personName),
            legalName = Some(PreviousName(Some(true), None, None, None)), // incomplete legal name
            lineId = Some(121212)
          )

          setupMocksFor(model)

          val result = controller.get(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
          )
        }
      }

      "respond with SEE_OTHER and show the registration progress page" when {
        "section data is unavailable" in new Fixture {
          when {
            controller.dataCacheConnector.fetchAll(any())
          } thenReturn Future.successful(None)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(None))

          val result = controller.get(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
        }

      }
    }

    "post is called" must {
      "redirect to YourResponsiblePeopleController" when {
        "fromYourAnswers is true and flow is not defined" which {
          "updates hasAccepted" in new Fixture {

            setupMocksFor(ResponsiblePerson(None, None))

            when {
              controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
            } thenReturn Future.successful(Cache.empty)

            val result = controller.post(1, None)(request)

            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
            )

            verify(controller.dataCacheConnector)
              .save(any(), any(), eqTo(Seq(ResponsiblePerson(hasAccepted = true))))(any())
          }
        }
      }
      "redirect to DetailedAnswersController" when {
        "for any other case" which {
          "updates hasAccepted" in new Fixture {

            val flow = None

            setupMocksFor(ResponsiblePerson(None, None))

            when {
              controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
            } thenReturn Future.successful(Cache.empty)

            val result = controller.post(1, flow)(request)

            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
            )

            verify(controller.dataCacheConnector)
              .save(any(), any(), eqTo(Seq(ResponsiblePerson(hasAccepted = true))))(any())

          }
        }
      }

      "redirect to RegisterPartnersController" when {
        "business type is partnership and flow is from declaration" in new Fixture {
          val flow = Some(`flowFromDeclaration`)

          val bm: BusinessMatching = businessMatchingGen.sample.get
          val rd                   = reviewDetailsGen.sample.get

          setupMocksFor(completeResponsiblePerson, SubmissionDecisionApproved)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
            .thenReturn(Future.successful(Some(Seq(completeResponsiblePerson))))

          when {
            controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          } thenReturn Future.successful(Cache.empty)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
            .thenReturn(Future.successful(Some(Seq(completeResponsiblePerson))))

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Future.successful(Some(bm.copy(reviewDetails = Some(rd.copy(businessType = Some(Partnership))))))
            )

          when(controller.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.post(1, flow)(request)
          redirectLocation(result) must be(Some(controllers.declaration.routes.RegisterPartnersController.get().url))
        }
      }

      "redirect to WhoIsRegisteringController" when {
        "business type is other than partnership, flow is from declaration and nominated officer is defined" in new Fixture {
          val flow = Some(`flowFromDeclaration`)

          val bm: BusinessMatching = businessMatchingGen.sample.get
          val rd                   = reviewDetailsGen.sample.get

          setupMocksFor(completeResponsiblePerson, SubmissionDecisionApproved)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
            .thenReturn(Future.successful(Some(Seq(completeResponsiblePerson))))

          when {
            controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          } thenReturn Future.successful(Cache.empty)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
            .thenReturn(Future.successful(Some(Seq(completeResponsiblePerson))))

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Future.successful(Some(bm.copy(reviewDetails = Some(rd.copy(businessType = Some(LimitedCompany))))))
            )

          when(controller.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.post(1, flow)(request)
          redirectLocation(result) must be(Some(declaration.routes.WhoIsRegisteringController.get.url))
        }
      }

      "redirect to WhoIsTheBusinessNominatedOfficerController in update service" when {
        "business type is other than partnership, flow is from declaration and nominated officer is not defined in pre submission" in new Fixture {
          val flow = Some(`flowFromDeclaration`)

          val bm: BusinessMatching = businessMatchingGen.sample.get
          val rd                   = reviewDetailsGen.sample.get

          setupMocksFor(completeResponsiblePerson, SubmissionReady)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
            .thenReturn(Future.successful(Some(Seq(completeResponsiblePerson))))

          when {
            controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          } thenReturn Future.successful(Cache.empty)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
            .thenReturn(
              Future.successful(
                Some(
                  Seq(
                    completeResponsiblePerson.copy(positions =
                      Some(Positions(Set(SoleProprietor), Some(PositionStartDate(LocalDate.now().minusMonths(5)))))
                    )
                  )
                )
              )
            )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Future.successful(Some(bm.copy(reviewDetails = Some(rd.copy(businessType = Some(LimitedCompany))))))
            )

          when(controller.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.post(1, flow)(request)
          redirectLocation(result) must be(Some(declaration.routes.WhoIsTheBusinessNominatedOfficerController.get.url))
        }
      }

      "redirect to WhoIsTheBusinessNominatedOfficerController in submit application" when {
        "business type is other than partnership, flow is from declaration and nominated officer is not defined in post submission" in new Fixture {
          val flow = Some(`flowFromDeclaration`)

          val bm: BusinessMatching = businessMatchingGen.sample.get
          val rd                   = reviewDetailsGen.sample.get

          setupMocksFor(completeResponsiblePerson, SubmissionDecisionApproved)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
            .thenReturn(Future.successful(Some(Seq(completeResponsiblePerson))))

          when {
            controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          } thenReturn Future.successful(Cache.empty)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any()))
            .thenReturn(
              Future.successful(
                Some(
                  Seq(
                    completeResponsiblePerson.copy(positions =
                      Some(Positions(Set(SoleProprietor), Some(PositionStartDate(LocalDate.now().minusMonths(5)))))
                    )
                  )
                )
              )
            )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Future.successful(Some(bm.copy(reviewDetails = Some(rd.copy(businessType = Some(LimitedCompany))))))
            )

          when(controller.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.post(1, flow)(request)
          redirectLocation(result) must be(Some(declaration.routes.WhoIsTheBusinessNominatedOfficerController.get.url))
        }
      }
    }
  }
}
