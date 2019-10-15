/*
 * Copyright 2019 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.businessmatching.BusinessMatching
import models.responsiblepeople._
import models.status._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class DetailedAnswersControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new DetailedAnswersController (
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mock[StatusService],
      config = mock[ApplicationConfig],
      cc = mockMcc
      )

    val businessMatching = BusinessMatching()

    val personName = PersonName("first name", None, "last name")

    def setupMocksFor(model: ResponsiblePerson, status: SubmissionStatus = SubmissionReady) = {

      when {
        controller.dataCacheConnector.fetchAll(any())(any())
      } thenReturn Future.successful(Some(mockCacheMap))

      when {
        mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any())
      } thenReturn Some(BusinessMatching())

      when {
        mockCacheMap.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
      } thenReturn Some(Seq(model))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any(), any()))
        .thenReturn(Future.successful(Some(BusinessMatching())))

      when(controller.statusService.getStatus(Some(any()), any(), any())(any(), any()))
        .thenReturn(Future.successful(status))
    }

  }

  "DetailedAnswersController" when {

    "get is called - from the yourResponsiblePeople controller" must {
      "respond with OK and show the detailed answers page with a 'confirm and continue'" in new Fixture {

        val model = ResponsiblePerson(None, None)
        setupMocksFor(model)

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        contentAsString(result) must include(Messages("title.cya"))
        contentAsString(result) must include("/anti-money-laundering/responsible-people/check-your-answers")
      }
    }

    "load yourAnswers page when the status is approved" in new Fixture {

      val model = ResponsiblePerson(Some(personName), None, lineId = Some(121212))
      setupMocksFor(model, SubmissionDecisionApproved)

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element = document.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
      element.hasAttr("href") must be(true)
    }

    "load yourAnswers page when the status is renewal submitted" in new Fixture {
      val model = ResponsiblePerson(Some(personName), None, lineId = Some(121212))

      setupMocksFor(model, RenewalSubmitted(None))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element = document.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
      element.hasAttr("href") must be(true)
    }

    "load yourAnswers page when the status is approved and has no lineId" in new Fixture {

      val model = ResponsiblePerson(Some(personName), None, lineId = None)
      setupMocksFor(model, SubmissionDecisionApproved)

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element = document.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
      element.hasAttr("href") must be(false)
    }

    "load yourAnswers page when the status is ready for renewal" in new Fixture {

      val model = ResponsiblePerson(Some(personName), lineId = Some(121212))
      setupMocksFor(model, ReadyForRenewal(None))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element = document.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
      element.hasAttr("href") must be(true)
    }

    "get is called - NOT from the yourAnswers controller" must {
      "respond with OK and show the detailed answers page with a 'confirm and continue'" in new Fixture {

        val model = ResponsiblePerson(None, None)
        setupMocksFor(model)

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        contentAsString(result) must include(Messages("title.cya"))
        contentAsString(result) must not include "/anti-money-laundering/responsible-people/your-answers"
      }
    }

    "get is called from any location" when {
      "section data is available" must {
        "respond with OK and show the detailed answers page with the correct title" in new Fixture {

          setupMocksFor(ResponsiblePerson(None, None))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include(Messages("title.cya"))
        }

        "respond with OK and show the detailed answers page with a correctly formatted responsiblePerson startDate" in new Fixture {

          private val testStartDate = new LocalDate(1999,1,1)

          val model = ResponsiblePerson(positions = Some(Positions(Set(BeneficialOwner),Some(PositionStartDate(testStartDate)))))
          setupMocksFor(model)

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include("1 January 1999")
        }
      }

      "respond with SEE_OTHER and show the registration progress page" when {
        "section data is unavailable" in new Fixture {
          when {
            controller.dataCacheConnector.fetchAll(any())(any())
          } thenReturn Future.successful(None)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
            (any(), any())).thenReturn(Future.successful(None))

          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
        }

      }
    }

    "post is called" must {
      "redirect to YourResponsiblePeopleController" when {
        "fromYourAnswers is true and flow is not defined" which {
          "updates hasAccepted" in new Fixture {

            setupMocksFor(ResponsiblePerson(None, None))

            when {
              controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any())
            } thenReturn Future.successful(CacheMap("", Map.empty))

            val result = controller.post(1, None)(request)

            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url))

            verify(controller.dataCacheConnector).save(any(), any(),eqTo(Seq(ResponsiblePerson(hasAccepted = true))))(any(), any())
          }
        }
      }
      "redirect to DetailedAnswersController" when {
        "for any other case" which {
          "updates hasAccepted" in new Fixture {

            val flow = None

            setupMocksFor(ResponsiblePerson(None, None))

            when {
              controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any())
            } thenReturn Future.successful(CacheMap("", Map.empty))

            val result = controller.post(1, flow)(request)

            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url))

            verify(controller.dataCacheConnector).save(any(), any(),eqTo(Seq(ResponsiblePerson(hasAccepted = true))))(any(),any())

          }
        }
      }
    }
  }
}
