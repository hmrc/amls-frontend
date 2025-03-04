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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.address.AdditionalExtraAddressFormProvider
import models.Country
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.AdditionalExtraAddressView

import scala.concurrent.Future

class AdditionalExtraAddressControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with BeforeAndAfter
    with OptionValues
    with Injecting {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId               = 1

  before {
    reset(mockDataCacheConnector)
  }

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    lazy val view                        = inject[AdditionalExtraAddressView]
    val additionalExtraAddressController = new AdditionalExtraAddressController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[AdditionalExtraAddressFormProvider],
      view = view,
      error = errorView
    )
  }

  val personName = Some(PersonName("firstname", None, "lastname"))

  val emptyCache = Cache.empty

  val mockCacheMap = mock[Cache]

  "AdditionalExtraAddressController" when {

    "get is called" must {

      "display the address country page" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the address page with pre-populated data for UK address" in new Fixture {

        val ukAddress = PersonAddressUK(
          "existingAddressLine1",
          Some("existingAddressLine1"),
          Some("existingAddressLine3"),
          Some("existingAddressLine4"),
          "AA11AA"
        )

        val responsiblePeople = ResponsiblePerson(
          personName = personName,
          addressHistory = Some(
            ResponsiblePersonAddressHistory(
              currentAddress = None,
              additionalAddress = None,
              additionalExtraAddress = Some(ResponsiblePersonAddress(ukAddress, None))
            )
          )
        )

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(true)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the address page with pre-populated data for non-UK address" in new Fixture {

        val ukAddress = PersonAddressNonUK(
          "existingAddressLine1",
          Some("existingAddressLine1"),
          Some("existingAddressLine3"),
          Some("existingAddressLine4"),
          Country("Spain", "ES")
        )

        val responsiblePeople = ResponsiblePerson(
          personName = personName,
          addressHistory = Some(
            ResponsiblePersonAddressHistory(
              currentAddress = None,
              additionalAddress = None,
              additionalExtraAddress = Some(ResponsiblePersonAddress(ukAddress, None))
            )
          )
        )

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
      }
    }

    "respond with NOT_FOUND" when {
      "address cannot be found" in new Fixture {
        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(NOT_FOUND)
      }
    }
  }

  "post is called" when {
    "form is valid" must {
      "go to AdditionalExtraAddressUKController" when {
        "user selected Yes" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "isUK" -> "true"
            )

          val responsiblePeople = ResponsiblePerson(personName)

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(
            additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.address.routes.AdditionalExtraAddressUKController.get(RecordId).url)
          )
        }
      }

      "go to AdditionalExtraAddressNonUKController" when {
        "user selected No" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "isUK" -> "false"
            )

          val responsiblePeople = ResponsiblePerson(personName)

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(
            additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.address.routes.AdditionalExtraAddressNonUKController.get(RecordId).url)
          )
        }
      }
    }

    "respond with BAD_REQUEST" when {
      "form is invalid" in new Fixture {
        val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressController.post(1).url)
          .withFormUrlEncodedBody()

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(
          additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
        )
          .thenReturn(Future.successful(mockCacheMap))

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
        status(result) must be(BAD_REQUEST)
      }
    }

    "process form as valid" when {
      "isUK is defined and false" in new Fixture {
        val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressController.post(1).url)
          .withFormUrlEncodedBody(
            "isUK" -> "false"
          )

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(
          additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
        )
          .thenReturn(Future.successful(mockCacheMap))

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.responsiblepeople.address.routes.AdditionalExtraAddressNonUKController.get(RecordId).url)
        )
      }

      "isUK is defined and true" in new Fixture {
        val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressController.post(1).url)
          .withFormUrlEncodedBody(
            "isUK" -> "true"
          )

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(
          additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
        )
          .thenReturn(Future.successful(mockCacheMap))

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.responsiblepeople.address.routes.AdditionalExtraAddressUKController.get(RecordId).url)
        )
      }
    }

    "redirect to AdditionalExtraAddressNonUK and wipe old address" when {
      "changed the answer from yes to no" in new Fixture {

        val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressController.post(1).url)
          .withFormUrlEncodedBody("isUK" -> "false")

        val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
        val currentAddress    = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
        val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
        val history           = ResponsiblePersonAddressHistory(
          currentAddress = Some(currentAddress),
          additionalAddress = Some(additionalAddress),
          additionalExtraAddress = Some(additionalAddress)
        )
        val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
        when(
          additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
        )
          .thenReturn(Future.successful(emptyCache))

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.AdditionalExtraAddressNonUKController.get(RecordId).url))

        val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
        verify(additionalExtraAddressController.dataCacheConnector)
          .save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), captor.capture())(any())
        captor.getValue.head.isComplete mustBe false
        captor.getValue.head.addressHistory.value.additionalExtraAddress mustBe Some(
          ResponsiblePersonAddress(PersonAddressNonUK("", None, None, None, Country("", "")), None)
        )
      }
    }

    "redirect to AdditionalExtraAddressUkController and wipe old address" when {
      "changed the answer from no to yes" in new Fixture {

        val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressController.post(1).url)
          .withFormUrlEncodedBody("isUK" -> "true")

        val ukAddress         = PersonAddressNonUK("Line 1", Some("Line 2"), Some("Line 3"), None, Country("", ""))
        val currentAddress    = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
        val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
        val history           = ResponsiblePersonAddressHistory(
          currentAddress = Some(currentAddress),
          additionalAddress = Some(additionalAddress),
          additionalExtraAddress = Some(additionalAddress)
        )
        val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
        when(
          additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
        )
          .thenReturn(Future.successful(emptyCache))

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.AdditionalExtraAddressUKController.get(RecordId).url))

        val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
        verify(additionalExtraAddressController.dataCacheConnector)
          .save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), captor.capture())(any())
        captor.getValue.head.isComplete mustBe false
        captor.getValue.head.addressHistory.value.additionalExtraAddress mustBe Some(
          ResponsiblePersonAddress(PersonAddressUK("", None, None, None, ""), None)
        )
      }
    }
  }
}
