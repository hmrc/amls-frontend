/*
 * Copyright 2021 HM Revenue & Customs
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

package models.hvd

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.Messages.Implicits._


class SalesChannelSpec extends WordSpec with MustMatchers with GuiceOneAppPerTest {
  import play.api.i18n._
  implicit val lang = Lang("en-US")

  "getMessage" must {
    "return correct text for Retail" in {
      Retail.getMessage must be("Retail")
    }

    "return correct text for Wholesale" in {
      Wholesale.getMessage must be("Wholesale")
    }

    "return correct text for Auction" in {
      Auction.getMessage must be("Auction")
    }
  }
}
