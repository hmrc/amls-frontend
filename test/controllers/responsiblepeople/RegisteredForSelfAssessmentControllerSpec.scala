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
import models.responsiblepeople.ResponsiblePeople._
import models.responsiblepeople.{PersonName, ResponsiblePeople, SaRegisteredYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class RegisteredForSelfAssessmentControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  val recordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new RegisteredForSelfAssessmentController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)
  val personName = Some(PersonName("firstname", None, "lastname"))

  "RegisteredForSelfAssessmentController" when {

    "get is called" must {
      "load the page with an empty form" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementById("saRegistered-true").hasAttr("checked") must be(false)
        document.getElementById("saRegistered-false").hasAttr("checked") must be(false)
      }

      "display the page with pre populated data" in new Fixture {

        val utr = "0123456789"

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName, saRegistered = Some(SaRegisteredYes(utr)))))))

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementById("saRegistered-true").hasAttr("checked") must be(true)
        document.getElementById("saRegistered-false").hasAttr("checked") must be(false)
        document.getElementById("utrNumber").`val` must be(utr)
      }

      "respond with NOT_FOUND when there is no responsiblePeople data" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(recordId)(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST when given invalid data" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "saRegistered" -> "test"
        )
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

        val result = controller.post(recordId)(newRequest)
        status(result) must be(BAD_REQUEST)

      }

      "respond with NOT_FOUND when the index is out of bounds" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "saRegistered" -> "true",
          "utrNumber" -> "0123456789"
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

        val result = controller.post(99)(newRequest)
        status(result) must be(NOT_FOUND)
      }

      "when edit is false" must {
        "redirect to the ExperienceTrainingController" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "saRegistered" -> "true",
            "utrNumber" -> "0123456789"
          )
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(recordId, edit = false)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ExperienceTrainingController.get(recordId).url))
        }
      }
      "when edit is true" must {
        "on post with valid data in edit mode" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "saRegistered" -> "true",
            "utrNumber" -> "0123456789"
          )
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(recordId, true, Some(flowFromDeclaration))(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(recordId, true, Some(flowFromDeclaration)).url))
        }
      }
    }
  }

  it must {
    "use correct services" in new Fixture {
      RegisteredForSelfAssessmentController.authConnector must be(AMLSAuthConnector)
      RegisteredForSelfAssessmentController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
