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

import generators.bankdetails.BankDetailsGenerator
import models.bankdetails.BankDetails
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks, StatusConstants}
import views.TitleValidator

class WhatYouNeedControllerSpec
  extends AmlsSpec
    with ScalaFutures
    with TitleValidator
    with BankDetailsGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)
    val controller = new WhatYouNeedController(self.authConnector, mockCacheConnector)

    def assertHref(url: String)(implicit doc: Document) = {
      doc.getElementById("bankwhatyouneed-button").attr("href") mustBe url
    }
  }

  "The GET action" must {
    "respond with SEE_OTHER and show the 'what you need' page" in new Fixture {
      mockCacheFetch(Gen.listOfN(3, bankDetailsGen).sample, Some(BankDetails.key))

      val result = controller.get()(request)

      status(result) must be(OK)

      implicit val doc = Jsoup.parse(contentAsString(result))
      validateTitle(s"${Messages("title.wyn")} - ${Messages("summary.bankdetails")}")

      contentAsString(result) must include(Messages("button.continue"))
    }

    "configure the link href correctly," which {
      "should link to the 'has bank accounts' page" when {
        "there are no bank accounts currently in the system" in new Fixture {
          mockCacheFetch[Seq[BankDetails]](None, Some(BankDetails.key))

          val result = controller.get()(request)

          implicit val doc = Jsoup.parse(contentAsString(result))

          assertHref(controllers.bankdetails.routes.HasBankAccountController.get().url)
        }

        "when there are bank accounts, but they've all been deleted" in new Fixture {
          mockCacheFetch[Seq[BankDetails]](Gen.listOfN(3, bankDetailsGen).sample map {
            _.map(_.copy(status = Some(StatusConstants.Deleted)))
          })

          val result = controller.get()(request)

          implicit val doc = Jsoup.parse(contentAsString(result))

          assertHref(controllers.bankdetails.routes.HasBankAccountController.get().url)
        }
      }

      "should link to the 'bank name' page" when {
        "there are already bank accounts in the system" in new Fixture {
          mockCacheFetch(Gen.listOfN(3, bankDetailsGen).sample, Some(BankDetails.key))

          val result = controller.get()(request)
          implicit val doc = Jsoup.parse(contentAsString(result))

          assertHref(controllers.bankdetails.routes.BankAccountNameController.getNoIndex.url)
        }
      }
    }
  }
}
