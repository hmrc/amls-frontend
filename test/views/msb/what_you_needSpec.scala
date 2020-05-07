/*
 * Copyright 2020 HM Revenue & Customs
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
import utils.AmlsViewSpec
import views.Fixture


class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
    def view = views.html.msb.what_you_need()
  }

  "What you need View" must {

    "have the back link button" in new ViewFixture {
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

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
      html must include(Messages("the amount of throughput you expect in the next 12 months"))
    }

    "state that branches or agents in other countries will be needed" in new ViewFixture {
      html must include(Messages("which countries you have branches or agents in, if you have any"))
    }
    "state system can identify linked transactions" in new ViewFixture {
      html must include(Messages("if your systems can identify linked transactions"))
    }

    "not display info that will not be needed" in new ViewFixture {
      html must not include Messages("your Intermediary Payment Service Provider’s name and Money Laundering Regulations number, if you use one")
      html must not include Messages("if you transfer money without using formal banking systems")
      html must not include Messages("the number of money transfers you expect to make in the next 12 months")
      html must not include Messages("which countries you expect to send the largest amounts of money to, if you send money to other countries")
      html must not include Messages("which countries you expect to send the most transactions to, if you send money to other countries")
      html must not include Messages("the number of currency exchange transactions you expect in the next 12 months")
      html must not include Messages("which currencies you expect to supply the most to your customers")
      html must not include Messages("who will supply your foreign currency, if you expect to deal in physical foreign currencies")
      html must not include Messages("the number foreign exchange transactions you expect in the next 12 months")
    }

    "Transmitting Money is a selected MSB subservice" when {
      trait TMViewFixture extends ViewFixture {
        override def view = views.html.msb.what_you_need(BusinessMatchingMsbServices(Set(TransmittingMoney)))
      }

      "provide correct content for TM" in new TMViewFixture {
        html must include(Messages("your Intermediary Payment Service Provider’s name and Money Laundering Regulations number, if you use one"))
        html must include(Messages("if you transfer money without using formal banking systems"))
        html must include(Messages("the number of money transfers you expect to make in the next 12 months"))
        html must include(Messages("which countries you expect to send the largest amounts of money to, if you send money to other countries"))
        html must include(Messages("which countries you expect to send the most transactions to, if you send money to other countries"))
      }
    }

    "Currency Exchange is a selected MSB subservice" when {
      trait CXViewFixture extends ViewFixture {
        override def view = views.html.msb.what_you_need(BusinessMatchingMsbServices(Set(CurrencyExchange)))
      }

      "provide correct content for currencyExchange" in new CXViewFixture {
        html must include(Messages("the number of currency exchange transactions you expect in the next 12 months"))
        html must include(Messages("which currencies you expect to supply the most to your customers"))
        html must include(Messages("who will supply your foreign currency, if you expect to deal in physical foreign currencies"))
      }
    }

    "Foreign Exchange is a selected MSB subservice" when {
      trait FXViewFixture extends ViewFixture {
        override def view = views.html.msb.what_you_need(BusinessMatchingMsbServices(Set(ForeignExchange)))
      }

      "state foreign exchange transactions info will be needed" in new FXViewFixture {
        html must include(Messages("the number foreign exchange transactions you expect in the next 12 months"))
      }
    }
  }
}
