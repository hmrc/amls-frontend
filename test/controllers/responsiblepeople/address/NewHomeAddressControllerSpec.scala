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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class NewHomeAddressControllerSpec extends AmlsSpec {

  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    val controller = new NewHomeAddressController(
      SuccessfulAuthAction,
      dataCacheConnector,
      commonDependencies,
      mockMcc
    )
  }

  val emptyCache = CacheMap("", Map.empty)
  val personName = Some(PersonName("firstname", None, "lastname"))

  "NewHomeAddressController" when {

    "get is called" must {

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {
        val responsiblePeople = ResponsiblePerson()

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controller.get(40)(request)
        status(result) must be(NOT_FOUND)
      }

      "display the new home address page successfully" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(controller.dataCacheConnector.fetch[NewHomeAddress](any(), meq(NewHomeAddress.key))
          (any(), any())).thenReturn(Future.successful(None))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)
      }

      "display the new home address page successfully with preloaded data for UK address" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(controller.dataCacheConnector.fetch[NewHomeAddress](any(), meq(NewHomeAddress.key))
          (any(), any())).thenReturn(Future.successful(Some(NewHomeAddress(PersonAddressUK("", "", None, None, "")))))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("isUK-true").hasAttr("checked") must be(true)
        document.getElementById("isUK-false").hasAttr("checked") must be(false)
      }

      "display the new home address page successfully with preloaded data for NonUK address" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(controller.dataCacheConnector.fetch[NewHomeAddress](any(), meq(NewHomeAddress.key))
          (any(), any())).thenReturn(Future.successful(Some(NewHomeAddress(PersonAddressNonUK("", "", None, None, Country("", ""))))))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("isUK-true").hasAttr("checked") must be(false)
        document.getElementById("isUK-false").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "save data and redirect to NewHomeAddressUKController" when {
        "yes selected" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "true"
          )

          val newHomeAddress = NewHomeAddress(PersonAddressUK("", "", None, None, ""))

          when(controller.dataCacheConnector.save[NewHomeAddress](any(), meq(NewHomeAddress.key), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.NewHomeAddressUKController.get(RecordId).url))
          verify(controller.dataCacheConnector).save[NewHomeAddress](any(), any(), meq(newHomeAddress))(any(), any())
        }
      }

      "save data and redirect to NewHomeAddressNonUKController" when {
        "no selected" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "false"
          )

          val newHomeAddress = NewHomeAddress(PersonAddressNonUK("", "", None, None, Country("", "")))

          when(controller.dataCacheConnector.save[NewHomeAddress](any(), meq(NewHomeAddress.key), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.NewHomeAddressNonUKController.get(RecordId).url))
          verify(controller.dataCacheConnector).save[NewHomeAddress](any(), any(), meq(newHomeAddress))(any(), any())
        }
      }

      "respond with BAD_REQUEST" when {
        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = requestWithUrlEncodedBody()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          when(controller.dataCacheConnector.save[NewHomeAddress](any(), meq(NewHomeAddress.key), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
}
