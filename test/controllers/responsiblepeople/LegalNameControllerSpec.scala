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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import generators.ResponsiblePersonGenerator
import models.changeofficer.{ChangeOfficer, NewOfficer, RoleInBusiness, SoleProprietor}
import models.responsiblepeople._
import models.responsiblepeople.ResponsiblePeople.flowChangeOfficer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalacheck.Gen
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper, StatusConstants}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


class LegalNameControllerSpec extends GenericTestHelper with ScalaFutures {

  trait TestFixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(self.authRequest)
    val RecordId = 1

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .build()

    lazy val controller = injector.instanceOf[LegalNameController]

    val emptyCache = CacheMap("", Map.empty)

  }

  "The LegalNameController" when {
    "get is called" must {
      "load the page" in new TestFixture {

        mockCacheFetch[Seq[ResponsiblePeople]](Some(Seq(ResponsiblePeople())))

        val result = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstName]").`val` must be("")
        document.select("input[name=middleName]").`val` must be("")
        document.select("input[name=lastName]").`val` must be("")
      }

      "prepopulate the view with data" in new TestFixture {

        val addPerson = PreviousName(
          firstName = Some("first"),
          middleName = Some("middle"),
          lastName = Some("last")
        )

        val responsiblePeople = ResponsiblePeople(legalName = Some(addPerson))

        mockCacheFetch[Seq[ResponsiblePeople]](Some(Seq(responsiblePeople)), Some(ResponsiblePeople.key))


        val result = controller.get(RecordId)(request)

        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=firstName]").`val` must be("first")
        document.select("input[name=middleName]").`val` must be("middle")
        document.select("input[name=lastName]").`val` must be("last")
      }

    }

    "post is called" must {
      "form is valid" must {
        "go to LegalNameController" when {
          "edit is false" in new TestFixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "firstName" -> "first",
              "middleName" -> "middle",
              "lastName" -> "last"
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

            when(controller.dataCacheConnector.save[PreviousName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(RecordId)(requestWithParams)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.LegalNameChangeDateController.get(RecordId).url))
          }
        }

        "go to DetailedAnswersController" when {
          "edit is true" in new TestFixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "firstName" -> "first",
              "middleName" -> "middle",
              "lastName" -> "last"
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

            when(controller.dataCacheConnector.save[PreviousName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(RecordId, true)(requestWithParams)
            status(result) must be(SEE_OTHER)
          }
        }
      }

      "form is invalid" must {
        "return BAD_REQUEST" in new TestFixture {

          val NameMissingInRequest = request.withFormUrlEncodedBody(
            "hasPreviousName" -> "true"
          )

          when(controller.dataCacheConnector.save[PreviousName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId)(NameMissingInRequest)
          status(result) must be(BAD_REQUEST)

        }

      }

      "model cannot be found with given index" must {
        "return NOT_FOUND" in new TestFixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "hasPreviousName" -> "true",
            "firstName" -> "first",
            "lastName" -> "last"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(2)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }

    }

  }

}
