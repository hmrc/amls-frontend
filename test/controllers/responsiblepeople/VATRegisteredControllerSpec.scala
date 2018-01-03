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

import connectors.DataCacheConnector
import models.responsiblepeople.ResponsiblePeople._
import models.responsiblepeople.{PersonName, ResponsiblePeople, VATRegisteredNo, VATRegisteredYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class VATRegisteredControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new VATRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val personName = Some(PersonName("firstname", None, "lastname"))

  "RegisteredForVATController" when {

    "get is called" must {
      "display the Registered for VAT page" when {
        "with pre populated data" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName,vatRegistered = Some(VATRegisteredYes("123456789")))))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[value=false]").hasAttr("checked") must be(false)
          document.select("input[value=true]").hasAttr("checked") must be(true)
          document.getElementById("vrnNumber").`val`() must be("123456789")
        }

        "without data" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

          val result = controller.get(1)(request)
          status(result) must be(OK)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("input[value=true]").hasAttr("checked") must be(false)
          document.select("input[value=false]").hasAttr("checked") must be(false)
        }
      }

      "display Not Found" when {
        "a populated ResponsiblePeople model cannot be found" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" when {

      "given valid data" must {
        "go to RegisteredForSelfAssessmentController" when {
          "edit = false" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForVAT" -> "true",
              "vrnNumber" -> "123456789",
              "personName" -> "Person Name"
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
              (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(vatRegistered = Some(VATRegisteredNo))))))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post(1)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1).url))
          }
        }

        "go to DetailedAnswersController" when {
          "edit = true" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForVAT" -> "true",
              "vrnNumber" -> "123456789",
              "personName" -> "Person Name"
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
              (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, true, Some(flowFromDeclaration)).url))
          }
        }
      }

      "given invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForVATYes" -> "1234567890",
              "personName" -> "Person Name"
            )
            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName)))))

            val result = controller.post(1)(newRequest)
            status(result) must be(BAD_REQUEST)
         }
      }

      "Responsible Person cannot be found with given index" must {
        "respond with NOT_FOUND" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "registeredForVAT" -> "true",
            "vrnNumber" -> "123456789",
            "personName" -> "Person Name"
          )
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName)))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(3)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }

    }
  }
}


