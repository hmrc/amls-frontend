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
import forms.responsiblepeople.address.AdditionalAddressFormProvider
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
import services.cache.Cache
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.AdditionalAddressView

import scala.concurrent.Future

class AdditionalAddressControllerSpec
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

    val auditConnector              = mock[AuditConnector]
    lazy val view                   = inject[AdditionalAddressView]
    val additionalAddressController = new AdditionalAddressController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[AdditionalAddressFormProvider],
      view = view,
      error = errorView
    )
  }

  val emptyCache  = Cache.empty
  val outOfBounds = 99

  "AdditionalAddressController" when {

    val pageTitle  = messages("responsiblepeople.additional_address.title", "firstname lastname") + " - " +
      messages("summary.responsiblepeople") + " - " +
      messages("title.amls") + " - " + messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "display the persons page when no existing data in mongoCache" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                                      must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the additionalAddressPage when existing data in mongoCache for UK address" in new Fixture {

        val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
        val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName, addressHistory = Some(history))

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                                      must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(true)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the additionalAddressPage when existing data in mongoCache for NonUK address" in new Fixture {
        val NonUKAddress      = PersonAddressNonUK("Line 1", Some("Line 2"), Some("Line 3"), None, Country("Norway", "NW"))
        val additionalAddress = ResponsiblePersonAddress(NonUKAddress, Some(ZeroToFiveMonths))
        val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName, addressHistory = Some(history))

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                                      must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked")  must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
      }

      "display 404 NotFound" when {

        "additionalAddress cannot be found" in new Fixture {

          val responsiblePeople = ResponsiblePerson()

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = additionalAddressController.get(RecordId)(request)
          status(result) must be(NOT_FOUND)
        }

      }
    }

    "post is called" must {
      "respond with BAD_REQUEST" when {
        "isUK field is not supplied" in new Fixture {
          val line1MissingRequest = FakeRequest(POST, routes.AdditionalAddressController.post(1).url)
            .withFormUrlEncodedBody()

          val responsiblePeople = ResponsiblePerson()

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#isUK]").html() must include(
            messages(s"error.required.uk.or.overseas.address.previous")
          )
        }
      }

      "go to AdditionalAddressUK" when {
        "isUK is true" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressController.post(1).url)
            .withFormUrlEncodedBody("isUK" -> "true")

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId, edit = true)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.address.routes.AdditionalAddressUKController.get(RecordId, true).url)
          )
        }
      }

      "go to AdditionalAddressNonUK page" when {
        "isUk is false" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressController.post(1).url)
            .withFormUrlEncodedBody("isUK" -> "false")

          val responsiblePeople = ResponsiblePerson()

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressNonUKController.get(RecordId).url))
        }
      }

      "redirect to AdditionalAddressNonUK and wipe old address" when {
        "changed the answer from yes to no" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressController.post(1).url)
            .withFormUrlEncodedBody("isUK" -> "false")

          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(
            currentAddress = Some(currentAddress),
            additionalAddress = Some(additionalAddress)
          )
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressNonUKController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
          verify(additionalAddressController.dataCacheConnector)
            .save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), captor.capture())(any())
          captor.getValue.head.isComplete mustBe false
          captor.getValue.head.addressHistory.value.additionalAddress mustBe Some(
            ResponsiblePersonAddress(PersonAddressNonUK("", None, None, None, Country("", "")), None)
          )
        }
      }

      "redirect to AdditionalAddressUkController and wipe old address" when {
        "changed the answer from no to yes" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressController.post(1).url)
            .withFormUrlEncodedBody("isUK" -> "true")

          val ukAddress         = PersonAddressNonUK("Line 1", Some("Line 2"), Some("Line 3"), None, Country("", ""))
          val currentAddress    = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(
            currentAddress = Some(currentAddress),
            additionalAddress = Some(additionalAddress)
          )
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressUKController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
          verify(additionalAddressController.dataCacheConnector)
            .save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), captor.capture())(any())
          captor.getValue.head.isComplete mustBe false
          captor.getValue.head.addressHistory.value.additionalAddress mustBe Some(
            ResponsiblePersonAddress(PersonAddressUK("", None, None, None, ""), None)
          )
        }
      }
    }
  }
}
