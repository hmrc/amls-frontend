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

package forms

import jto.validation.Path
import jto.validation.ValidationError

trait FormHelpers {

  trait MessageFilter {
    val messages: Seq[String]
  }

  case object StandardMessageFilter extends MessageFilter {
    override val messages = Seq(
      "error.expected.jodadate.format",
      "error.future.date"
    )
  }

  implicit val standardFilter = StandardMessageFilter

  implicit class InvalidFormExtensions(form: InvalidForm) {
    def withMessageFor(p: Path, message: String)(implicit exceptions: MessageFilter) = {
      form.errors.exists(f => f._1 == p && f._2.map(_.message).intersect(exceptions.messages).isEmpty) match {
        case true => InvalidForm(form.data, (form.errors filter (x => x._1 != p)) :+ (p, Seq(ValidationError(message))))
        case _ => form
      }
    }
  }

}
