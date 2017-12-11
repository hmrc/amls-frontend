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

import models.bankdetails._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper, StatusConstants}

class RemoveBankDetailsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new RemoveBankDetailsController (
      dataCacheConnector =  mockCacheConnector,
      authConnector = self.authConnector
    )
  }

  "Get" must {

    "load the remove bank account page when section data is available" in new Fixture {

      mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))))

      val result = controller.get(1,false)(request)

      status(result) must be(NOT_FOUND)
    }

    "show bank account details on the remove bank account page" in new Fixture {

      mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None, Some(BankAccount("Account Name", NonUKAccountNumber("12345678")))))))

      val result = controller.get(1,true) (request)

      val contentString = contentAsString(result)

      val pageTitle = Messages("bankdetails.remove.bank.account.title") + " - " +
        Messages("summary.bankdetails") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      val document = Jsoup.parse(contentString)
      document.title() mustBe pageTitle
    }

    "remove bank account from summary" in new Fixture {

      val emptyCache = CacheMap("", Map.empty)

      val accountType1 = PersonalAccount
      val bankAccount1 = BankAccount("My Account1", UKAccount("111111", "11-11-11"))

      val accountType2 = PersonalAccount
      val bankAccount2 = BankAccount("My Account2", UKAccount("222222", "22-22-22"))

      val accountType3 = PersonalAccount
      val bankAccount3 = BankAccount("My Account3", UKAccount("333333", "33-33-33"))

      val accountType4 = PersonalAccount
      val bankAccount4 = BankAccount("My Account4", UKAccount("444444", "44-44-44"))

      val completeModel1 = BankDetails(Some(accountType1), None, Some(bankAccount1), true, false, Some(StatusConstants.Deleted))
      val completeModel2 = BankDetails(Some(accountType2), None, Some(bankAccount2))
      val completeModel3 = BankDetails(Some(accountType3), None, Some(bankAccount3))
      val completeModel4 = BankDetails(Some(accountType4), None, Some(bankAccount4))

      val bankAccounts = Seq(completeModel1,completeModel2,completeModel3,completeModel4)

      mockCacheFetch[Seq[BankDetails]](Some(bankAccounts))
      mockCacheSave[Seq[BankDetails]]

      val result = controller.remove(1)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(controllers.bankdetails.routes.SummaryController.get(false).url))

      verify(controller.dataCacheConnector).save[Seq[BankDetails]](
        any(),
        meq(Seq(completeModel1, completeModel2,completeModel3,completeModel4))
      )(any(), any(), any())

    }

  }
}
