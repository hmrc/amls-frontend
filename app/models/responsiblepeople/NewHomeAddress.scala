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

package models.responsiblepeople

import jto.validation.{From, Rule, To, Write}
import jto.validation.forms.UrlFormEncoded

case class NewHomeAddress(personAddress: PersonAddress)

object NewHomeAddress {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, NewHomeAddress] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._
      __.read[PersonAddress] map NewHomeAddress.apply _

  }

  def addressFormRule(paFormRule: Rule[UrlFormEncoded, PersonAddress]): Rule[UrlFormEncoded, NewHomeAddress] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._
    __.read(paFormRule) map NewHomeAddress.apply _

  }

  implicit val formWrites: Write[NewHomeAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    __.write[PersonAddress] contramap{x =>x.personAddress}
  }

  implicit val format = Json.format[NewHomeAddress]

}