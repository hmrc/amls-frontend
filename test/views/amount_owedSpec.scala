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

package views

import play.api.i18n.Messages
import utils.AmlsSpec

class amount_owedSpec extends AmlsSpec {

  "amount_owed view" must {

    "have correct title, headings and content" in new Fixture {

      def view = views.html.amount_owed(20, "test")

      doc.title must be(Messages("payments.you.owe.title")
        + " - " + Messages("status.title")
        + " - " + Messages("title.amls")
        + " - " + Messages("title.gov"))

      heading.html must be(Messages("payments.you.owe.heading") + "20")
      subHeading.html must include(Messages("status.title"))

      html must include(Messages("payments.you.owe.info"))
      html must include(Messages("payments.you.owe.info2"))
    }
  }
}