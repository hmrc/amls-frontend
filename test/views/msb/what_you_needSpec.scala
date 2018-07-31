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

package views.msb

import models.businessmatching.{BusinessMatchingMsbServices, CurrencyExchange, ForeignExchange, TransmittingMoney}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture


class what_you_needSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
    def view = views.html.msb.what_you_need()
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("title.wyn"))
    }

    "have the correct Headings" in new ViewFixture{
      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.msb"))
    }

    "have an introduction to the list of information needed" in new ViewFixture{
      html must include(Messages("msb.whatyouneed.requiredinfo.heading"))
    }

    "state that throughput info will be needed" in new ViewFixture {
      html must include(Messages("msb.whatyouneed.line_1"))
    }

    "state that branches or agents in other countries will be needed" in new ViewFixture {
      html must include(Messages("msb.whatyouneed.line_2"))
    }

    "not display info that will not be needed" in new ViewFixture {
      html must not include Messages("msb.whatyouneed.line_3")
      html must not include Messages("msb.whatyouneed.line_4")
      html must not include Messages("msb.whatyouneed.line_5")
      html must not include Messages("msb.whatyouneed.line_6")
      html must not include Messages("msb.whatyouneed.line_7")
      html must not include Messages("msb.whatyouneed.line_8")
    }

    "Transmitting Money is a selected MSB subservice" when {
      trait TMViewFixture extends ViewFixture {
        override def view = views.html.msb.what_you_need(BusinessMatchingMsbServices(Set(TransmittingMoney)))
      }

      "state that where you expect to send money/transactions will be needed" in new TMViewFixture {
        html must include(Messages("msb.whatyouneed.line_3"))
      }

      "state that IPSP info will be needed" in new TMViewFixture {
        html must include(Messages("msb.whatyouneed.line_8"))
      }
    }

    "Currency Exchange is a selected MSB subservice" when {
      trait CXViewFixture extends ViewFixture {
        override def view = views.html.msb.what_you_need(BusinessMatchingMsbServices(Set(CurrencyExchange)))
      }

      "state which currencies you supply most of will be needed" in new CXViewFixture {
        html must include(Messages("msb.whatyouneed.line_4"))
      }

      "state if you deal in physical foreign currencies will be needed" in new CXViewFixture {
        html must include(Messages("msb.whatyouneed.line_5"))
      }

      "state currency exchange transactions info will be needed" in new CXViewFixture {
        html must include(Messages("msb.whatyouneed.line_6"))
      }
    }

    "Foreign Exchange is a selected MSB subservice" when {
      trait FXViewFixture extends ViewFixture {
        override def view = views.html.msb.what_you_need(BusinessMatchingMsbServices(Set(ForeignExchange)))
      }

      "state foreign exchange transactions info will be needed" in new FXViewFixture {
        html must include(Messages("msb.whatyouneed.line_7"))
      }
    }
  }
}
