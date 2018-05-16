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

package generators.submission

import generators.BaseGenerator
import models.confirmation.{BreakdownRow, Currency}
import org.scalacheck.Gen

//scalastyle:off magic.number
trait BreakdownGenerator extends BaseGenerator {

  def breakdownRowGen: Gen[BreakdownRow] = for {
    label <- stringOfLengthGen(10)
    quantity <- Gen.choose(1, 10)
  } yield BreakdownRow(label, quantity, Currency(100), Currency(quantity * 100))

}
