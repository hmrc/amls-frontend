/*
 * Copyright 2022 HM Revenue & Customs
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

package views.include

import forms.ValidField
import jto.validation.{KeyPathNode, Path}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import utils.AmlsViewSpec
import views.html.include.forms2.textarea

class textAreaSpec extends PlaySpec with AmlsViewSpec {

  trait Fixture {
  }

  "The Html output" must {
    "render the correct hint text value when text is entered" in new Fixture {
      val result: String = textarea(ValidField(Path(List(KeyPathNode("key"))), Seq(""))).toString()
      val html: Document = Jsoup.parse(result)
      println(html)

      val hintText: String = html.getElementById("key-info").text()

      hintText mustBe "You can enter up to 200 characters"
    }
  }
}
