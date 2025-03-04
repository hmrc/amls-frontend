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

package views.msb

import controllers.msb.routes.ExpectedThroughputController
import models.businessmatching.BusinessMatchingMsbServices
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange, TransmittingMoney}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  private val call                                               = ExpectedThroughputController.get()
  lazy val what_you_need                                         = inject[WhatYouNeedView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {
    def view = what_you_need(call)
  }

  "What you need View" must {

    "Have the correct title" in new ViewFixture {
      doc.title must startWith(messages("title.wyn"))
    }

    "have the correct Headings" in new ViewFixture {
      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.msb"))
    }

    behave like pageWithBackLink(what_you_need(call))

    "have an introduction to the list of information needed" in new ViewFixture {
      html must include(messages("msb.whatyouneed.requiredinfo.heading"))
    }

    "state that throughput info will be needed" in new ViewFixture {
      html must include(messages("msb.whatyouneed.line_1"))
    }

    "state that branches or agents in other countries will be needed" in new ViewFixture {
      html must include(messages("which countries you have branches or agents in, if you have any"))
    }
    "state system can identify linked transactions" in new ViewFixture {
      html must include(messages("if your systems can identify linked transactions"))
    }

    "not display info that will not be needed" in new ViewFixture {
      html must not include messages(
        "your Intermediary Payment Service Provider’s name and Money Laundering Regulations number, if you use one"
      )
      html must not include messages("if you transfer money without using formal banking systems")
      html must not include messages("the number of money transfers you expect to make in the next 12 months")
      html must not include messages(
        "which countries you expect to send the largest amounts of money to, if you send money to other countries"
      )
      html must not include messages(
        "which countries you expect to send the most transactions to, if you send money to other countries"
      )
      html must not include messages("the number of currency exchange transactions you expect in the next 12 months")
      html must not include messages("which currencies you expect to supply the most to your customers")
      html must not include messages(
        "who will supply your foreign currency, if you expect to deal in physical foreign currencies"
      )
      html must not include messages("the number foreign exchange transactions you expect in the next 12 months")
    }

    "Transmitting Money is a selected MSB subservice" when {
      trait TMViewFixture extends ViewFixture {
        override def view = what_you_need(call, BusinessMatchingMsbServices(Set(TransmittingMoney)))
      }

      "provide correct content for TM" in new TMViewFixture {
        html must include(
          messages(
            "your Intermediary Payment Service Provider’s name and Money Laundering Regulations number, if you use one"
          )
        )
        html must include(messages("if you transfer money without using formal banking systems"))
        html must include(messages("the number of money transfers you expect to make in the next 12 months"))
        html must include(
          messages(
            "which countries you expect to send the largest amounts of money to, if you send money to other countries"
          )
        )
        html must include(
          messages("which countries you expect to send the most transactions to, if you send money to other countries")
        )
      }
    }

    "Currency Exchange is a selected MSB subservice" when {
      trait CXViewFixture extends ViewFixture {
        override def view = what_you_need(call, BusinessMatchingMsbServices(Set(CurrencyExchange)))
      }

      "provide correct content for currencyExchange" in new CXViewFixture {
        html must include(messages("the number of currency exchange transactions you expect in the next 12 months"))
        html must include(messages("which currencies you expect to supply the most to your customers"))
        html must include(
          messages("who will supply your foreign currency, if you expect to deal in physical foreign currencies")
        )
      }
    }

    "Foreign Exchange is a selected MSB subservice" when {
      trait FXViewFixture extends ViewFixture {
        override def view = what_you_need(call, BusinessMatchingMsbServices(Set(ForeignExchange)))
      }

      "state foreign exchange transactions info will be needed" in new FXViewFixture {
        html must include(messages("the number foreign exchange transactions you expect in the next 12 months"))
      }
    }
  }
}
