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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BankAccountAddControllerSpec extends GenericTestHelper
  with MockitoSugar
  with BeforeAndAfter {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new BankAccountAddController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(Seq(BankDetails(
        Some(PersonalAccount),
        Some(BankAccount("AccountName", UKAccount("12341234", "121212"))))))))
    when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(emptyCache))
  }

  val emptyCache = CacheMap("", Map.empty)


  "BankAccountAddController" when {
    "get is called" must {
      "respond with SEE_OTHER" when {
        "display guidance is true" in new Fixture {

          val result = controller.get(true)(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhatYouNeedController.get(2).url))
        }

        "display guidance is false" in new Fixture {

          val result = controller.get(false)(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountTypeController.get(2, false).url))
        }
      }
    }
  }
}
