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

package models.payments

import jto.validation.Valid
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.AmlsSpec

class WaysToPaySpec extends PlaySpec with AmlsSpec  with MockitoSugar {

  "Form Validation" must {

    "successfully validate" when {
      "on selecting card" in {

        WaysToPay.formRule.validate(Map("waysToPay" -> Seq(WaysToPay.Card.entryName))) must be(Valid(WaysToPay.Card))

        WaysToPay.formRule.validate(Map("waysToPay" -> Seq(WaysToPay.Bacs.entryName))) must be(Valid(WaysToPay.Bacs))

      }
    }

    "write correct data from enum" in {

      WaysToPay.formWrites.writes(WaysToPay.Card) must
        be(Map("waysToPay" -> Seq(WaysToPay.Card.entryName)))

      WaysToPay.formWrites.writes(WaysToPay.Bacs) must
        be(Map("waysToPay" -> Seq(WaysToPay.Bacs.entryName)))

    }

  }

}
