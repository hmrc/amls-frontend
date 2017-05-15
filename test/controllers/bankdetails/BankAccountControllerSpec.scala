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

package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BankAccountControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new BankAccountController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override implicit val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)
  val fieldElements = Array("accountName", "accountNumber", "sortCode", "IBANNumber")

  "BankAccountController" when {
    "get is called" must {
      "respond with OK" when {
        "there is no bank account detail information yet" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.get(1, false)(request)
          val document: Document = Jsoup.parse(contentAsString(result))

          status(result) must be(OK)
          for (field <- fieldElements)
            document.select(s"input[name=$field]").`val` must be(empty)
        }

        "there is already bank account detail information" in new Fixture {
          val ukBankAccount = BankAccount("My Account", UKAccount("12345678", "000000"))

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, Some(ukBankAccount))))))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.get(1, true)(request)
          status(result) must be(OK)
          // check the radio buttons are checked
        }
      }

      "respond with NOT_FOUND" when {
        "there is no bank account information at all" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.get(1, false)(request)

          status(result) must be(NOT_FOUND)
        }
        "editing an amendment" in new Fixture {

          val ukBankAccount = BankAccount("My Account", UKAccount("12345678", "000000"))

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, Some(ukBankAccount))))))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = controller.get(1, true)(request)

          status(result) must be(NOT_FOUND)

        }
        "editing a variaton" in new Fixture {

          val ukBankAccount = BankAccount("My Account", UKAccount("12345678", "000000"))

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, Some(ukBankAccount))))))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.get(1, true)(request)

          status(result) must be(NOT_FOUND)

        }
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "given valid data in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "false",
            "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
            "isIBAN" -> "false"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(false).url))
        }
        "given valid data when NOT in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "false",
            "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
            "isIBAN" -> "false"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, false)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountRegisteredController.get(1).url))
        }
        "blahblah given valid data in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "false",
            "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
            "isIBAN" -> "false"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(false).url))
        }
      }

      "respond with NOT_FOUND" when {
        "given an index out of bounds in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "false",
            "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
            "isIBAN" -> "false"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(50, true)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }


      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "true"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
            (any(), any(), any())).thenReturn(Future.successful(None))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)

          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
}
