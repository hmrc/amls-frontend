/*
 * Copyright 2019 HM Revenue & Customs
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

package views.supervision

import org.scalatest.MustMatchers
import utils.AmlsSpec
import views.Fixture


class what_you_needSpec extends AmlsSpec with MustMatchers  {

  "what_you_need view" must {

    "have a back link" in new Fixture {

      def view = views.html.supervision.what_you_need()

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
