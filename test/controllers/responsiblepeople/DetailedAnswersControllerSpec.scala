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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.responsiblepeople.{BeneficialOwner, PersonName, Positions, ResponsiblePeople}
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved, SubmissionReady}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class DetailedAnswersControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new DetailedAnswersController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    val model = ResponsiblePeople(None, None)

    val personName = PersonName("first name", None, "last name")

    when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(Seq(model))))

    when(controller.statusService.getStatus(any(), any(), any()))
      .thenReturn(Future.successful(SubmissionReady))
  }

  "DetailedAnswersController" when {

    "get is called - from the yourAnswers controller" must {
      "respond with OK and show the detailed answers page with a 'confirm and continue'" in new Fixture {

        val result = controller.get(1, true)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        contentAsString(result) must include(Messages("responsiblepeople.detailed_answers.title"))
        contentAsString(result) must not include "/anti-money-laundering/responsible-people/check-your-answers"
      }
    }

    "load yourAnswers page when the status is approved" in new Fixture {
      override val model = ResponsiblePeople(Some(personName), None, lineId = Some(121212))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.get(1, true)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element = document.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
      element.hasAttr("href") must be(true)
    }

    "load yourAnswers page when the status is renewal submitted" in new Fixture {
      override val model = ResponsiblePeople(Some(personName), None, lineId = Some(121212))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(RenewalSubmitted(None)))

      val result = controller.get(1, true)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element = document.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
      element.hasAttr("href") must be(true)
    }

    "load yourAnswers page when the status is approved and has no lineId" in new Fixture {
      override val model = ResponsiblePeople(Some(personName), None, lineId = None)

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.get(1, true)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element = document.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
      element.hasAttr("href") must be(false)
    }

    "load yourAnswers page when the status is ready for renewal" in new Fixture {
      override val model = ResponsiblePeople(Some(personName), lineId = Some(121212))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(ReadyForRenewal(None)))

      val result = controller.get(1, true)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      val element = document.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
      element.hasAttr("href") must be(true)
    }

    "get is called - NOT from the yourAnswers controller" must {
      "respond with OK and show the detailed answers page with a 'confirm and continue'" in new Fixture {

        override val model = ResponsiblePeople(None, None)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))

        val result = controller.get(1, false)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        contentAsString(result) must include(Messages("responsiblepeople.detailed_answers.title"))
        contentAsString(result) must not include "/anti-money-laundering/responsible-people/your-answers"
      }
    }

    "get is called from any location" when {
      "section data is available" must {
        "respond with OK and show the detailed answers page with the correct title" in new Fixture {

          val result = controller.get(1, false)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include(Messages("responsiblepeople.detailed_answers.title"))
        }

        "respond with OK and show the detailed answers page with a correctly formatted responsiblePerson startDate" in new Fixture {

          private val testStartDate = new LocalDate(1999,1,1)

          override val model = ResponsiblePeople(positions = Some(Positions(Set(BeneficialOwner),Some(testStartDate))))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(model))))

          val result = controller.get(1, false)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include("1 January 1999")
        }
      }

      "respond with SEE_OTHER and show the registration progress page" when {
        "section data is unavailable" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(None))
          val result = controller.get(1, false)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
        }

      }
    }

    "post is called" must {
      "redirect to YourAnswersController" when {
        "fromYourAnswers is true and flow is not defined" which {
          "updates hasAccepted" in new Fixture {

            when {
              controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(),any())(any(),any(),any())
            } thenReturn Future.successful(CacheMap("", Map.empty))

            val result = controller.post(1, true, None)(request)

            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.YourAnswersController.get().url))

            verify(controller.dataCacheConnector).save(any(),eqTo(Seq(model.copy(hasAccepted = true))))(any(),any(),any())
          }
        }
      }
      "redirect to SummaryController" when {
        "for any other case" which {
          "updates hasAccepted" in new Fixture {

            val flow = None

            when {
              controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(),any())(any(),any(),any())
            } thenReturn Future.successful(CacheMap("", Map.empty))

            val result = controller.post(1, false, flow)(request)

            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SummaryController.get(flow).url))

            verify(controller.dataCacheConnector).save(any(),eqTo(Seq(model.copy(hasAccepted = true))))(any(),any(),any())

          }
        }
      }
    }
  }

  it must {
    "use the correct services" in new Fixture {
      DetailedAnswersController.authConnector must be(AMLSAuthConnector)
      DetailedAnswersController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
