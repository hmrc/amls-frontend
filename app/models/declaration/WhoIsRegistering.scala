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

package models.declaration

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class WhoIsRegistering(person : String) {
  val indexValue = """([0-9]+)$""".r.findFirstIn(person) map {_.toInt}
}

object WhoIsRegistering {

  import utils.MappingUtils.Implicits._

  val key = "who-is-registering"

  implicit val formRule: Rule[UrlFormEncoded, WhoIsRegistering] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "person").read[String].withMessage("error.required.declaration.who.is.registering") map WhoIsRegistering.apply
    }
  implicit val formWrites: Write[WhoIsRegistering, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
        (__ \ "person").write[String] contramap{x =>x.person}
  }

  implicit val format = Json.format[WhoIsRegistering]

}
