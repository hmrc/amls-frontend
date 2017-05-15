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

package utils

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class DateHelperSpec extends UnitSpec with MockitoSugar  {
  "isNotFutureDate" must {
    "return false if the date is later than current date" in {
      DateHelper.isNotFutureDate(LocalDate.now().plusDays(1)) shouldBe false
    }

    "return true if the date is equal to or earlier than the current date" in {
      DateHelper.isNotFutureDate(LocalDate.now()) shouldBe true
      DateHelper.isNotFutureDate(LocalDate.now().minusDays(1)) shouldBe true
    }
  }
}