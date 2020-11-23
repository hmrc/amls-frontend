/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{ContactDetails, PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.responsiblepeople.contact_details

import scala.concurrent.Future

class ContactDetailsControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    self =>
    val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[contact_details]
    val controller = new ContactDetailsController (
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc,
      contact_details = view,
      error = errorView)
  }

  val emptyCache = CacheMap("", Map.empty)

  "ContactDetailsController" when {

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {
      "display the contact details page" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName)))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=phoneNumber]").`val` must be("")
        document.select("input[name=emailAddress]").`val` must be("")

      }

      "display the contact details page with pre populated data" in new Fixture {

        val contact = ContactDetails("07702745869", "test@test.com")
        val res = ResponsiblePerson(personName = personName, contactDetails = Some(contact))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(res))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=phoneNumber]").`val` must be(contact.phoneNumber)
        document.select("input[name=emailAddress]").`val` must be(contact.emailAddress)
      }

      "respond with NOT_FOUND" when {
        "there is no responsible person for the index" in new Fixture {
          val contact = ContactDetails("07000000000", "test@test.com")
          val res = ResponsiblePerson(personName = personName, contactDetails = Some(contact))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(res))))

          val result = controller.get(0)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" when {
      "given a valid form" when {
          "go to CurrentAddressController" in new Fixture {

            val newRequest = requestWithUrlEncodedBody(
              "phoneNumber" -> "07000000000",
              "emailAddress" -> "test@test.com"
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(), ResponsiblePerson()))))

            when(controller.dataCacheConnector.save[ContactDetails](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(2)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.CurrentAddressController.get(2).url))
          }

        "there is no responsible person for the index" must {
          "respond with NOT_FOUND" in new Fixture {

            val newRequest = requestWithUrlEncodedBody(
              "phoneNumber" -> "07000000000",
              "emailAddress" -> "test@test.com"
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(), ResponsiblePerson()))))

            when(controller.dataCacheConnector.save[ContactDetails](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(0)(newRequest)
            status(result) must be(NOT_FOUND)
          }
        }
        "in edit mode" must {
          "go to DetailedAnswersController" in new Fixture {

            val newRequest = requestWithUrlEncodedBody(
              "phoneNumber" -> "07000000000",
              "emailAddress" -> "test@test.com"
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
              (any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

            when(controller.dataCacheConnector.save[ContactDetails](any(), any(), any())
              (any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post(1, true,Some(flowFromDeclaration))(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
          }
        }
      }

      "given an invalid form" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "phoneNumber" -> "<070>00000000",
            "emailAddress" -> "test@test.com"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
            (any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName)))))

          when(controller.dataCacheConnector.save[ContactDetails](any(), any(), any())
            (any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }
    }
  }
}


