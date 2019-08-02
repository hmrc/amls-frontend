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

package controllers.changeofficer

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthAction, AuthorisedFixture, StatusConstants}

import scala.concurrent.Future

class RemoveResponsiblePersonControllerSpec extends AmlsSpec with MockitoSugar {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)

    val dataCacheConnector = mock[DataCacheConnector]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .build()

    lazy val controller = injector.instanceOf[RemoveResponsiblePersonController]

    val nominatedOfficer = ResponsiblePerson(
      personName = Some(PersonName("firstName", None, "lastName")),
      positions = Some(Positions(Set(NominatedOfficer),None))
    )

    val otherResponsiblePerson = ResponsiblePerson(
      personName = Some(PersonName("otherFirstName", None, "otherLastName")),
      positions = Some(Positions(Set(Director),None))
    )

    when {
      dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any())
    } thenReturn Future.successful(Some(Seq(nominatedOfficer, otherResponsiblePerson)))

    when {
      controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(),any(), any())( any(), any())
    } thenReturn Future.successful(CacheMap("", Map.empty))

  }

  "The RemoveResponsiblePersonController" when {

    "get is called" must {
      "display the view" in new TestFixture {
        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include(Messages("changeofficer.removeresponsibleperson.title"))
      }

      "return INTERNAL_SERVER_ERROR" when {
        "nominated officer name cannot be found" in new TestFixture {

          when {
            dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any())
          } thenReturn Future.successful(None)

          val result = controller.get()(request)

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "post is called" must {
      "Redirect to NewOfficerController" in new TestFixture {

        val result = controller.post()(request.withFormUrlEncodedBody(
          "date.day" -> "10",
          "date.month" -> "11",
          "date.year" -> "2001"
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.changeofficer.routes.NewOfficerController.get().url)

      }
      "return BAD_REQUEST for invalid form" in new TestFixture {
        val result = controller.post()(request.withFormUrlEncodedBody(
          "date.day" -> "a",
          "date.month" -> "b",
          "date.year" -> "c"
        ))

        status(result) mustBe BAD_REQUEST
      }

      "return INTERNAL_SERVER_ERROR" when {
        "nominated officer name cannot be found" when {
          "invalid form is submitted" in new TestFixture {

            when {
              dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(),any())( any(), any())
            } thenReturn Future.successful(None)

            val result = controller.post()(request)

            status(result) mustBe INTERNAL_SERVER_ERROR
          }
          "valid form is submitted" in new TestFixture {

            when {
              dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(),any())( any(), any())
            } thenReturn Future.successful(None)

            val result = controller.post()(request.withFormUrlEncodedBody(
              "date.day" -> "a",
              "date.month" -> "b",
              "date.year" -> "c"
            ))

            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }

  }

  it must {
    "save the responsible person as deleted given an end date" in new TestFixture {
      val result = controller.post()(request.withFormUrlEncodedBody(
        "date.day" -> "10",
        "date.month" -> "11",
        "date.year" -> "2001"
      ))

      status(result) mustBe SEE_OTHER

      verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](any(), any(), meq(
        Seq(
          nominatedOfficer.copy(
            endDate = Some(ResponsiblePersonEndDate(new LocalDate(2001,11,10))),
            status = Some(StatusConstants.Deleted),
            hasChanged = true
          ),
          otherResponsiblePerson
        )
      ))(any(),any())

    }
  }
}