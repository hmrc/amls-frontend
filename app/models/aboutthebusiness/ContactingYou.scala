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

package models.aboutthebusiness

import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}
import play.api.libs.json.Json

case class ContactingYou(
                          phoneNumber: Option[String] = None,
                          email: Option[String] = None
                        )

object ContactingYou {

  implicit val formats = Json.format[ContactingYou]

  implicit val formWrites: Write[ContactingYou, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import scala.Function.unlift
    (
      (__ \ "phoneNumber").write[Option[String]] ~
        (__ \ "email").write[Option[String]]
      ) (unlift(ContactingYou.unapply _))
  }
}
