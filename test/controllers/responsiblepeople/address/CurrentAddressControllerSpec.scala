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
import forms.responsiblepeople.address.CurrentAddressFormProvider
import models.Country
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.HeaderCarrier
import services.cache.Cache
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.CurrentAddressView

import scala.concurrent.Future

class CurrentAddressControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with OptionValues
    with BeforeAndAfter
    with Injecting {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockDataCacheConnector     = mock[DataCacheConnector]
  val RecordId                   = 1
  val emptyCache                 = Cache.empty
  val outOfBounds                = 99

  before {
    reset(mockDataCacheConnector)
  }

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    lazy val view                = inject[CurrentAddressView]
    val currentAddressController = new CurrentAddressController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[CurrentAddressFormProvider],
      view = view,
      error = errorView
    )

  }

  "CurrentAddressController" when {

    val pageTitle = messages("responsiblepeople.wherepersonlives.title", "firstname lastname") + " - " +
      messages("summary.responsiblepeople") + " - " +
      messages("title.amls") + " - " + messages("title.gov")

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {
        val responsiblePeople = ResponsiblePerson()

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(40)(request)
        status(result) must be(NOT_FOUND)
      }

      "display the address country page when no existing data in mongoCache" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                                      must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the address country page with yes selected when UK address is stored in mongo" in new Fixture {

        val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
        val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                                      must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(true)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the address country page with no selected when non-UK address is stored in mongo" in new Fixture {

        val nonUkAddress      =
          PersonAddressNonUK("Line 1", Some("Line 2"), Some("Line 3"), None, Country("NZ", "New Zealand"))
        val additionalAddress = ResponsiblePersonCurrentAddress(nonUkAddress, Some(ZeroToFiveMonths))
        val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                                      must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "redirect to CurrentAddressUkController" when {
        "true selected" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressController.post(1).url)
            .withFormUrlEncodedBody("isUK" -> "true")

          val responsiblePeople = ResponsiblePerson()

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CurrentAddressUKController.get(RecordId).url))
        }
      }

      "redirect to CurrentAddressNonUkController and wipe old address" when {
        "changed the answer from yes to no" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressController.post(1).url)
            .withFormUrlEncodedBody("isUK" -> "false")

          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(
            currentAddress = Some(currentAddress),
            additionalAddress = Some(additionalAddress)
          )
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CurrentAddressNonUKController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
          verify(currentAddressController.dataCacheConnector)
            .save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), captor.capture())(any())
          captor.getValue.head.isComplete mustBe false
          captor.getValue.head.addressHistory.value.currentAddress mustBe Some(
            ResponsiblePersonCurrentAddress(
              PersonAddressNonUK("", None, None, None, Country("", "")),
              Some(ZeroToFiveMonths),
              None
            )
          )
        }
      }

      "redirect to CurrentAddressUkController and wipe old address" when {
        "changed the answer from no to yes" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressController.post(1).url)
            .withFormUrlEncodedBody("isUK" -> "true")

          val ukAddress         = PersonAddressNonUK("Line 1", Some("Line 2"), Some("Line 3"), None, Country("", ""))
          val currentAddress    = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(
            currentAddress = Some(currentAddress),
            additionalAddress = Some(additionalAddress)
          )
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CurrentAddressUKController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
          verify(currentAddressController.dataCacheConnector)
            .save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), captor.capture())(any())
          captor.getValue.head.isComplete mustBe false
          captor.getValue.head.addressHistory.value.currentAddress mustBe Some(
            ResponsiblePersonCurrentAddress(PersonAddressUK("", None, None, None, ""), Some(ZeroToFiveMonths), None)
          )
        }
      }

      "redirect to CurrentAddressNonUkController" when {
        "false selected" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressController.post(1).url)
            .withFormUrlEncodedBody("isUK" -> "false")

          val responsiblePeople = ResponsiblePerson()

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CurrentAddressNonUKController.get(RecordId).url))
        }
      }

      "respond with BAD_REQUEST" when {

        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = FakeRequest(POST, routes.CurrentAddressController.post(1).url)
            .withFormUrlEncodedBody()

          val responsiblePeople = ResponsiblePerson(personName)

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(currentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(mock[Cache]))

          val result = currentAddressController.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#isUK]").html() must include(
            messages(s"error.required.uk.or.overseas.address.current", personName.get.titleName)
          )
        }
      }
    }
  }
}
