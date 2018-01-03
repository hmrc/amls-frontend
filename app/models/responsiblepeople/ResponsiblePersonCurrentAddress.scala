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

package models.responsiblepeople

import models.DateOfChange
import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}


case class ResponsiblePersonCurrentAddress(personAddress: PersonAddress,
                                           timeAtAddress: Option[TimeAtAddress],
                                           dateOfChange: Option[DateOfChange] = None)

object ResponsiblePersonCurrentAddress {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonCurrentAddress] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._
    (
      __.read[PersonAddress] ~ (__ \ "timeAtAddress").read[Option[TimeAtAddress]]
      ) ((personAddress:PersonAddress, _:Option[TimeAtAddress]) => ResponsiblePersonCurrentAddress(personAddress, None, None))
  }

  def unapplyNoDateOfChange(currentAddress:ResponsiblePersonCurrentAddress):Option[(PersonAddress,Option[TimeAtAddress])] =
    Some((currentAddress.personAddress,currentAddress.timeAtAddress))

  implicit val formWrites: Write[ResponsiblePersonCurrentAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      __.write[PersonAddress] ~
        __.write[Option[TimeAtAddress]]
      ) (unlift(ResponsiblePersonCurrentAddress.unapplyNoDateOfChange))
  }

  implicit val format = Json.format[ResponsiblePersonCurrentAddress]

}
