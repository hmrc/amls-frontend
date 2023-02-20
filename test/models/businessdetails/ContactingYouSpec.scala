/*
 * Copyright 2023 HM Revenue & Customs
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

package models.businessdetails

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class ContactingYouSpec extends PlaySpec with MockitoSugar {
  "Contacting You Form Details" must {

    "write correct data" in {
      val model = ContactingYou(Some("1234567890"), Some("test@test.com"))
      ContactingYou.formWrites.writes(model) must
        be(Map(
          "phoneNumber" -> Seq("1234567890"),
          "email" -> Seq("test@test.com")
        ))
    }

  }
}