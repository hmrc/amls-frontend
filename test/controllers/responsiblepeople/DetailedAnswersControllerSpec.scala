/*
 * Copyright 2018 HM Revenue & Customs
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

import config.{AMLSAuthConnector, AppConfig}
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.responsiblepeople.{BeneficialOwner, PersonName, Positions, ResponsiblePerson}
import models.status._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class DetailedAnswersControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new DetailedAnswersController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
      override val config = mock[AppConfig]
    }

    val businessMatching = BusinessMatching()

    val personName = PersonName("first name", None, "last name")

    def setupMocksFor(model: ResponsiblePerson, status: SubmissionStatus = SubmissionReady) = {

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      when(controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(BusinessMatching())))

      when(controller.statusService.getStatus(any(), any(), any()))
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

          val model = ResponsiblePerson(positions = Some(Positions(Set(BeneficialOwner),Some(testStartDate))))
          setupMocksFor(model)

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include("1 January 1999")
        }
      }

      "respond with SEE_OTHER and show the registration progress page" when {
        "section data is unavailable" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())
            (any(), any(), any())).thenReturn(Future.successful(None))
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
              controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(),any())(any(),any(),any())
            } thenReturn Future.successful(CacheMap("", Map.empty))

            val result = controller.post(1, None)(request)

            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url))

            verify(controller.dataCacheConnector).save(any(),eqTo(Seq(ResponsiblePerson(hasAccepted = true))))(any(),any(),any())
          }
        }
      }
      "redirect to DetailedAnswersController" when {
        "for any other case" which {
          "updates hasAccepted" in new Fixture {

            val flow = None

            setupMocksFor(ResponsiblePerson(None, None))

            when {
              controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(),any())(any(),any(),any())
            } thenReturn Future.successful(CacheMap("", Map.empty))

            val result = controller.post(1, flow)(request)

            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url))

            verify(controller.dataCacheConnector).save(any(),eqTo(Seq(ResponsiblePerson(hasAccepted = true))))(any(),any(),any())

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
