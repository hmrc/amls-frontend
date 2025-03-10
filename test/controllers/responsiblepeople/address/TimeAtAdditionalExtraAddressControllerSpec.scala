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
import forms.responsiblepeople.address.TimeAtAddressFormProvider
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.responsiblepeople.address.TimeAtAdditionalExtraAddressView

import scala.concurrent.Future

class TimeAtAdditionalExtraAddressControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId               = 1

  trait Fixture {
    self =>
    val request                                = addToken(authRequest)
    lazy val view                              = inject[TimeAtAdditionalExtraAddressView]
    val timeAtAdditionalExtraAddressController = new TimeAtAdditionalExtraAddressController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[TimeAtAddressFormProvider],
      view = view,
      error = errorView
    )
  }

  val mockCacheMap = mock[Cache]
  val emptyCache   = Cache.empty
  val outOfBounds  = 99

  "TimeAtAdditionalExtraAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {
      "display status 200" when {
        "without existing data" in new Fixture {

          val responsiblePeople = ResponsiblePerson(personName)

          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          )
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalExtraAddressController.get(RecordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          TimeAtAddress.all.foreach { item =>
            document.getElementById(item.toString).hasAttr("checked") must be(false)
          }
        }

        "with existing data" in new Fixture {

          val personName = Some(PersonName("firstname", None, "lastname"))

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          )
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalExtraAddressController.get(RecordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          TimeAtAddress.all.foreach { item =>
            val checkboxIsChecked = document.getElementById(item.toString).hasAttr("checked")

            if (item == ZeroToFiveMonths) checkboxIsChecked must be(true) else checkboxIsChecked must be(false)
          }
        }
      }

      "respond with NOT_FOUND" when {
        "called with an index that is out of bounds" in new Fixture {

          val responsiblePeople = ResponsiblePerson()

          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          )
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalExtraAddressController.get(outOfBounds)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {

      "go to DetailedAnswersController" when {
        "edit is true" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtAdditionalExtraAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> ZeroToFiveMonths.toString
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          )
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(
              any()
            )
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result =
            timeAtAdditionalExtraAddressController.post(RecordId, true, Some(flowFromDeclaration))(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.routes.DetailedAnswersController
                .get(RecordId, Some(flowFromDeclaration))
                .url
            )
          )
        }
      }

      "go to PositionWithinBusinessController" when {
        "edit is false" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtAdditionalExtraAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> ZeroToFiveMonths.toString
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          )
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(
              any()
            )
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.PositionWithinBusinessController.get(RecordId).url)
          )
        }
      }

      "respond with BAD_REQUEST on submission" when {

        "given an invalid form" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtAdditionalExtraAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> ""
            )

          val responsiblePeople = ResponsiblePerson()

          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          )
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(
              any()
            )
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId, true)(requestWithParams)

          status(result) mustBe BAD_REQUEST

        }

      }

      "respond with NOT_FOUND" when {
        "an addressExtraAddress is not stored for that index" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.TimeAtAdditionalExtraAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> ZeroToFiveMonths.toString
            )

          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          )
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
          when(
            timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(
              any()
            )
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
