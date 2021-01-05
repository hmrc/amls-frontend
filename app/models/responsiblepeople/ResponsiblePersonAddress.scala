/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{From, Rule, To, ValidationError, Write}
import jto.validation.forms._


case class ResponsiblePersonAddress(personAddress: PersonAddress,
                                    timeAtAddress: Option[TimeAtAddress])

object ResponsiblePersonAddress {

  import play.api.libs.json._

  val validateCountry: Rule[PersonAddress, PersonAddress] = Rule.fromMapping[PersonAddress, PersonAddress] {
    case address: PersonAddressNonUK if address.country.code == "GB" => Invalid(Seq(ValidationError(List("error.required.select.non.uk.previous.address"))))
    case address => Valid(address)
  }

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonAddress] = From[UrlFormEncoded] { __ =>

  import jto.validation.forms.Rules._
    (__.read(validateCountry) ~ (__ \ "timeAtAddress").read[Option[TimeAtAddress]]) (ResponsiblePersonAddress.apply _)
}

  def addressFormRule(paFormRule: Rule[UrlFormEncoded, PersonAddress]): Rule[UrlFormEncoded, ResponsiblePersonAddress] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._
    (__.read(paFormRule andThen validateCountry) ~ (__ \ "timeAtAddress").read[Option[TimeAtAddress]]) (ResponsiblePersonAddress.apply _)
  }

  implicit val formWrites: Write[ResponsiblePersonAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
        __.write[PersonAddress] ~
          __.write[Option[TimeAtAddress]]
      ) (unlift(ResponsiblePersonAddress.unapply))
  }

  implicit val format = Json.format[ResponsiblePersonAddress]

}

