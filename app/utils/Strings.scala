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

import play.twirl.api.Html

object Strings {

  implicit class ConsoleHelpers(s: String) {
    def in(colour: String) =s"$colour$s${Console.RESET}"
  }

  trait LineBreakConverter {
    def convert(input: String): String
  }

  implicit val defaultLineBreakConverter = new LineBreakConverter {
    override def convert(input: String) = input.replaceAll("""\s*\n\s*""", "</p><p>")
  }

  implicit class TextHelpers(s: String) {
    def convertLineBreaks(implicit converter: LineBreakConverter) = converter.convert(s)
    def convertLineBreaksH(implicit converter: LineBreakConverter) = Html(converter.convert(s))
    def paragraphize(implicit converter: LineBreakConverter) = s"<p>${converter.convert(s)}</p>"
    def paragraphizeH(implicit converter: LineBreakConverter) = Html(s"<p>${converter.convert(s)}</p>")
  }

}
