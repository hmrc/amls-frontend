/*
 * Copyright 2018 HM Revenue & Customs
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
import models.bankdetails.{BankDetails, PersonalAccount}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BankAccountRegisteredControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new BankAccountRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BankAccountRegisteredController" must {

    "Get Option:" must {

      "load the Bank account Registered page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val pageTitle = Messages("bankdetails.bank.account.registered.title") + " - " +
          Messages("summary.bankdetails") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe pageTitle
      }

      "load the Bank account Registered page1" in new Fixture {
        val accountType = PersonalAccount

        when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(BankDetails(Some(accountType),None), BankDetails(Some(accountType),None)))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("bankdetails.have.registered.accounts.text", 2))
      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes'" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("registerAnotherBank" -> "true")

        when(controller.dataCacheConnector.fetch[BankDetails](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BankDetails](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.bankdetails.routes.BankAccountAddController.get(false).url))
      }

      "successfully redirect to the page on selection of 'no'" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("registerAnotherBank" -> "false")

        when(controller.dataCacheConnector.fetch[BankDetails](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BankDetails](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.bankdetails.routes.SummaryController.get(false).url))
      }
    }

    "on post invalid data show error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()
      when(controller.dataCacheConnector.fetch[BankDetails](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("bankdetails.want.to.register.another.account"))

    }

    "on post with invalid data show error" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "registerAnotherBank" -> ""
      )
      when(controller.dataCacheConnector.fetch[BankDetails](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("bankdetails.want.to.register.another.account"))

    }
  }
}
