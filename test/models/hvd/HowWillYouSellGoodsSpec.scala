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

package models.hvd

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.Json


class HowWillYouSellGoodsSpec extends WordSpec with MustMatchers{

  val fullData = HowWillYouSellGoods(Seq(Wholesale, Retail, Auction))
  val fullForm = Map (
      "salesChannels[]" -> Seq("Wholesale","Retail","Auction")
  )

  "How will You Sell Goods" should {
    "Round trip through Json" in {
      val j = Json.toJson(fullData)
      j.as[HowWillYouSellGoods] must be (fullData)
    }

    "write to the expected form"  in {
      HowWillYouSellGoods.formW.writes(fullData)  must be (fullForm)
    }

    "read from the expected form"  in {
      HowWillYouSellGoods.formR.validate(fullForm)  must be (Valid(fullData))
    }

    "round trip through Url encoded form" in {
      HowWillYouSellGoods.formW.writes(
        HowWillYouSellGoods.formR.validate(fullForm) match {
          case Valid(x) => x
          case _ => HowWillYouSellGoods(Seq.empty)
        }
      ) must be (fullForm)

      HowWillYouSellGoods.formR.validate(
        HowWillYouSellGoods.formW.writes(fullData)
      ) must be (Valid(fullData))
    } 

    "fail form validation of no channels are selected" in {
      val testForm = Map[String,Seq[String]]()

      HowWillYouSellGoods.formR.validate(testForm) must be (Invalid(Seq((Path \ "salesChannels") -> Seq(ValidationError("error.required.hvd.how-will-you-sell-goods")))))
    }
  }
}
