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

package controllers.bankdetails

import controllers.actions.SuccessfulAuthAction
import generators.bankdetails.BankDetailsGenerator
import models.bankdetails.BankDetails
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks, StatusConstants}
import views.TitleValidator
import views.html.bankdetails.WhatYouNeedView

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends AmlsSpec with ScalaFutures with TitleValidator with BankDetailsGenerator {

  trait Fixture extends DependencyMocks {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    lazy val whatYouNeed: WhatYouNeedView        = app.injector.instanceOf[WhatYouNeedView]

    val controller =
      new WhatYouNeedController(SuccessfulAuthAction, commonDependencies, mockCacheConnector, mockMcc, whatYouNeed)

    def assertHref(url: String)(implicit doc: Document): Assertion =
      doc.getElementById("button").attr("href") mustBe url
  }

  "The GET action" must {
    "respond with SEE_OTHER and show the 'what you need' page" in new Fixture {
      mockCacheFetch(Gen.listOfN(3, bankDetailsGen).sample, Some(BankDetails.key))

      val result: Future[Result] = controller.get()(request)

      status(result) must be(OK)

      implicit val doc: Document = Jsoup.parse(contentAsString(result))
      validateTitle(s"${Messages("title.wyn")} - ${Messages("summary.bankdetails")}")

      contentAsString(result) must include(Messages("button.continue"))
    }

    "remove the itemIndex from session if there was one present" in new Fixture {
      override val request: Request[AnyContentAsEmpty.type] = addTokenWithSessionParam(authRequest)("itemIndex" -> "4")

      mockCacheFetch(Gen.listOfN(3, bankDetailsGen).sample, Some(BankDetails.key))

      val result: Future[Result] = controller.get()(request)

      status(result) must be(OK)
      session(result).get("itemIndex") mustBe None
    }

    "configure the link href correctly," which {
      "should link to the 'has bank accounts' page" when {
        "there are no bank accounts currently in the system" in new Fixture {
          mockCacheFetch[Seq[BankDetails]](None, Some(BankDetails.key))

          val result: Future[Result] = controller.get()(request)

          implicit val doc: Document = Jsoup.parse(contentAsString(result))

          assertHref(controllers.bankdetails.routes.HasBankAccountController.get.url)
        }

        "when there are bank accounts, but they've all been deleted" in new Fixture {
          mockCacheFetch[Seq[BankDetails]](Gen.listOfN(3, bankDetailsGen).sample map {
            _.map(_.copy(status = Some(StatusConstants.Deleted)))
          })

          val result: Future[Result] = controller.get()(request)

          implicit val doc: Document = Jsoup.parse(contentAsString(result))

          assertHref(controllers.bankdetails.routes.HasBankAccountController.get.url)
        }
      }

      "should link to the 'bank name' page" when {
        "there are already bank accounts in the system" in new Fixture {
          mockCacheFetch(Gen.listOfN(3, bankDetailsGen).sample, Some(BankDetails.key))

          val result: Future[Result] = controller.get()(request)
          implicit val doc: Document = Jsoup.parse(contentAsString(result))

          assertHref(controllers.bankdetails.routes.BankAccountNameController.getNoIndex.url)
        }
      }
    }
  }
}
