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
import forms.responsiblepeople.address.AdditionalAddressUKFormProvider
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.AmlsSpec
import views.html.responsiblepeople.address.AdditionalExtraAddressUKView

import scala.concurrent.Future

class AdditionalExtraAddressUKControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId               = 1

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val auditConnector                   = mock[AuditConnector]
    lazy val view                        = inject[AdditionalExtraAddressUKView]
    val additionalExtraAddressController = new AdditionalExtraAddressUKController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      auditConnector = auditConnector,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[AdditionalAddressUKFormProvider],
      view = view,
      error = errorView
    )

    when {
      auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)
  }

  val personName = Some(PersonName("firstname", None, "lastname"))

  val emptyCache = Cache.empty

  val mockCacheMap = mock[Cache]

  "AdditionalExtraAddressController" when {

    "get is called" must {

      "display the extra address UK page" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=addressLine1]").`val` must be("")
        document.select("input[name=addressLine2]").`val` must be("")
        document.select("input[name=addressLine3]").`val` must be("")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=postCode]").`val`     must be("")

      }
      "display the persons page with pre-populated data" in new Fixture {

        val address = PersonAddressUK(
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
              None,
              None,
              Some(ResponsiblePersonAddress(address, None))
            )
          )
        )

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val doc = Jsoup.parse(contentAsString(result))
        doc.getElementById("addressLine1").`val`() mustBe address.addressLine1
        doc.getElementById("addressLine2").`val`() mustBe address.addressLine2.get
        doc.getElementById("addressLine3").`val`() mustBe address.addressLine3.get
        doc.getElementById("addressLine4").`val`() mustBe address.addressLine4.get
        doc.getElementById("postCode").`val`() mustBe address.postCode

      }
    }

    "respond with NOT_FOUND" when {
      "name cannot be found" in new Fixture {
        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(NOT_FOUND)
      }
    }
  }

  "post is called" when {

    "form is valid" must {
      "go to TimeAtAdditionalExtraAddressController" when {
        "edit is false" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode"     -> "AA1 1AA"
            )

          val ukAddress              = PersonAddressUK("Line 1", Some("Line 2"), None, None, "AA1 1AA")
          val additionalAddress      = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val additionalExtraAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
          val history                = ResponsiblePersonAddressHistory(
            currentAddress = Some(additionalAddress),
            additionalExtraAddress = Some(additionalExtraAddress)
          )
          val responsiblePeople      = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(
            additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.address.routes.TimeAtAdditionalExtraAddressController.get(RecordId).url)
          )

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Line 1"
              d.detail("addressLine2") mustBe "Line 2"
              d.detail("postCode") mustBe "AA1 1AA"
          }
        }

        "edit is true and timeAtAddress does not exist" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode"     -> "AA1 1AA"
            )

          val ukAddress              = PersonAddressUK("Line 1", Some("Line 2"), None, None, "AA1 1AA")
          val additionalAddress      = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val additionalExtraAddress = ResponsiblePersonAddress(ukAddress, None)
          val history                = ResponsiblePersonAddressHistory(
            currentAddress = Some(additionalAddress),
            additionalExtraAddress = Some(additionalExtraAddress)
          )
          val responsiblePeople      = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(
            additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = additionalExtraAddressController.post(RecordId, true)(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.address.routes.TimeAtAdditionalExtraAddressController
                .get(RecordId, true)
                .url
            )
          )
        }
      }

      "go to DetailedAnswersController" when {
        "edit is true" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "New line 1",
              "addressLine2" -> "New line 2",
              "postCode"     -> "TE1 1ET"
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(
            additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result =
            additionalExtraAddressController.post(RecordId, true, Some(flowFromDeclaration))(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.routes.DetailedAnswersController
                .get(RecordId, Some(flowFromDeclaration))
                .url
            )
          )

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "New line 1"
              d.detail("addressLine2") mustBe "New line 2"
              d.detail("postCode") mustBe "TE1 1ET"
              d.detail("originalLine1") mustBe "Line 1"
              d.detail("originalLine2") mustBe "Line 2"
              d.detail("originalPostCode") mustBe "AA1 1AA"
          }
        }
      }

    }

    "respond with BAD_REQUEST" when {
      "form is invalid" in new Fixture {
        val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLineNonUK1" -> "",
            "addressLineNonUK2" -> "",
            "country"           -> ""
          )

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

    "respond with NOT_FOUND" when {
      "responsible person is not found for that index" in new Fixture {
        val requestWithParams = FakeRequest(POST, routes.AdditionalExtraAddressUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode"     -> "AA1 1AA"
          )

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(
          additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
        )
          .thenReturn(Future.successful(mockCacheMap))

        val result = additionalExtraAddressController.post(0)(requestWithParams)
        status(result) must be(NOT_FOUND)
      }
    }
  }
}
