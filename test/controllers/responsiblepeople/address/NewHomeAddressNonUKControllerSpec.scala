/*
 * Copyright 2023 HM Revenue & Customs
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
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import models.{Country, DateOfChange}
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, AutoCompleteServiceMocks}
import views.html.responsiblepeople.address.new_home_address_NonUK

import scala.concurrent.Future

class NewHomeAddressNonUKControllerSpec extends AmlsSpec with AutoCompleteServiceMocks {

  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]
    lazy val view = app.injector.instanceOf[new_home_address_NonUK]
    val controller = new NewHomeAddressNonUKController(
      SuccessfulAuthAction,
      dataCacheConnector,
      mockAutoComplete,
      commonDependencies,
      mockMcc,
      new_home_address_NonUK = view,
      error = errorView
    )
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99
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

      "display the new home UK address page successfully" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)
      }
    }

    "post is called" must {
      "redirect to DetailedAnswersController" when {
        "all the mandatory non-UK parameters are supplied" in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "new address line1",
            "country" -> "ES"
          )

          val nonUKAddress = PersonAddressNonUK("new address line1", None, None, None, Country("Spain","ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(nonUKAddress, Some(OneToThreeYears), Some(DateOfChange(LocalDate.now().minusMonths(13))))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), meq(ResponsiblePerson.key))(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controller.dataCacheConnector.fetch[NewHomeDateOfChange](any(), meq(NewHomeDateOfChange.key))(any(), any()))
            .thenReturn(Future.successful(Some(NewHomeDateOfChange(Some(LocalDate.now().minusMonths(13))))))

          when(controller.dataCacheConnector.save[ResponsiblePerson](any(), meq(ResponsiblePerson.key), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(controller.dataCacheConnector.save[NewHomeDateOfChange](any(), meq(NewHomeDateOfChange.key), any())
            (any(), any())).thenReturn(Future.successful(emptyCache))

          when(controller.dataCacheConnector.removeByKey[NewHomeAddress](any(), meq(NewHomeAddress.key))
            (any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId).url))
          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](any(), any(), meq(Seq(responsiblePeople)))(any(), any())
        }

        "all the mandatory non-UK parameters are supplied and date of move is more then 6 months" in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "new address line1",
            "country" -> "ES"
          )

          val ukAddress = PersonAddressUK("Line 111", None, None, None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths), Some(DateOfChange(LocalDate.now().minusMonths(7))))
          val nCurrentAddress = ResponsiblePersonCurrentAddress(PersonAddressNonUK("new address line1", None, None, None, Country("Spain","ES")), Some(SixToElevenMonths), Some(DateOfChange(LocalDate.now().minusMonths(7))))

          val additionalAddress = ResponsiblePersonAddress(PersonAddressUK("Line 11", None, None, None, "AB1 1BA"), Some(ZeroToFiveMonths))
          val additionalExtraAddress = ResponsiblePersonAddress(PersonAddressUK("Line 21", None, None, None, "BB1 1BB"), Some(ZeroToFiveMonths))

          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress),
            additionalAddress = Some(additionalAddress),
            additionalExtraAddress = Some(additionalExtraAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          val pushCurrentToAdditional = ResponsiblePersonAddress(PersonAddressUK("Line 111", None, None, None, "AA1 1AA"), Some(ZeroToFiveMonths))
          val pushCurrentToExtraAdditional = ResponsiblePersonAddress(PersonAddressUK("Line 11", None, None, None, "AB1 1BA"), Some(ZeroToFiveMonths))


          val upDatedHistory = ResponsiblePersonAddressHistory(currentAddress = Some(nCurrentAddress),
            additionalAddress = Some(pushCurrentToAdditional),
            additionalExtraAddress = Some(pushCurrentToExtraAdditional))
          val nResponsiblePeople = ResponsiblePerson(addressHistory = Some(upDatedHistory), hasChanged = true)


          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), meq(ResponsiblePerson.key))(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controller.dataCacheConnector.fetch[NewHomeDateOfChange](any(), meq(NewHomeDateOfChange.key))(any(), any()))
            .thenReturn(Future.successful(Some(NewHomeDateOfChange(Some(LocalDate.now().minusMonths(7))))))

          when(controller.dataCacheConnector.save[ResponsiblePerson](any(), meq(ResponsiblePerson.key), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(controller.dataCacheConnector.save[NewHomeDateOfChange](any(), meq(NewHomeDateOfChange.key), any())
            (any(), any())).thenReturn(Future.successful(emptyCache))

          when(controller.dataCacheConnector.removeByKey[NewHomeAddress](any(), meq(NewHomeAddress.key))
            (any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId).url))
          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](any(), any(), meq(Seq(nResponsiblePeople)))(any(), any())
        }

        "all the mandatory non-UK parameters are supplied and date of move is more then 3 years" in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "new address line1",
            "country" -> "ES"
          )

          val newCurrentAddress = ResponsiblePersonCurrentAddress(PersonAddressNonUK("new address line1", None, None, None, Country("Spain","ES")), Some(ThreeYearsPlus), Some(DateOfChange(LocalDate.now().minusMonths(37))))
          val ukAddress1 = PersonAddressUK("Line 11", None, None, None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(ukAddress1, Some(ThreeYearsPlus), Some(DateOfChange(LocalDate.now().minusMonths(37))))
          val additionalAddress = ResponsiblePersonAddress(PersonAddressUK("Line 11", None, None, None, "AB1 1BA"), Some(ZeroToFiveMonths))
          val additionalExtraAddress = ResponsiblePersonAddress(PersonAddressUK("Line 21", None, None, None, "BB1 1BB"), Some(ZeroToFiveMonths))

          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress),
            additionalAddress = Some(additionalAddress),
            additionalExtraAddress = Some(additionalExtraAddress))

          val responsiblePeople1 = ResponsiblePerson(addressHistory = Some(history))
          val updatedHistory = ResponsiblePersonAddressHistory(currentAddress = Some(newCurrentAddress), additionalAddress = None, additionalExtraAddress = None)

          when(controller.dataCacheConnector.fetch[NewHomeDateOfChange](any(), meq(NewHomeDateOfChange.key))(any(), any()))
            .thenReturn(Future.successful(Some(NewHomeDateOfChange(Some(LocalDate.now().minusMonths(37))))))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), meq(ResponsiblePerson.key))(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople1))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), meq(ResponsiblePerson.key),
            any())(any(), any())).thenReturn(Future.successful(emptyCache))

          when(controller.dataCacheConnector.save[NewHomeDateOfChange](any(), meq(NewHomeDateOfChange.key), any())
            (any(), any())).thenReturn(Future.successful(emptyCache))

          when(controller.dataCacheConnector.removeByKey[NewHomeAddress](any(), meq(NewHomeAddress.key))
            (any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId).url))
          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](any(), any(),
            meq(Seq(responsiblePeople1.copy(addressHistory = Some(updatedHistory), hasChanged = true))))(any(), any())
        }
      }

      "respond with BAD_REQUEST" when {

        "given an invalid address" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "Line &1",
            "addressLineNonUK2" -> "Line *2",
            "country" -> "ES"
          )
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          val result = controller.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)
        }

        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = requestWithUrlEncodedBody()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          when(controller.dataCacheConnector.save[ResponsiblePerson](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          val result = controller.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)
        }

        "the default fields for overseas are not supplied" in new Fixture {

          val requestWithMissingParams = requestWithUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "",
            "addressLineNonUK2" -> "",
            "country" -> ""
          )
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
          val result = controller.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

        }

        "respond with NOT_FOUND" when {
          "given an out of bounds index" in new Fixture {
            val requestWithParams = requestWithUrlEncodedBody(
              "isUK" -> "false",
              "addressLineNonUK1" -> "Line 1",
              "addressLineNonUK2" -> "Line 2",
              "country" -> "ES"
            )

            val ukAddress = PersonAddressNonUK("line1", Some("line2"), None, None, Country("Spain","ES"))
            val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(controller.dataCacheConnector.save[ResponsiblePerson](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(outOfBounds)(requestWithParams)

            status(result) must be(NOT_FOUND)
          }
        }
      }
    }
  }
}


