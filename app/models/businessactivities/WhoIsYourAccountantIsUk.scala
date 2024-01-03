/*
 * Copyright 2024 HM Revenue & Customs
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

package models.businessactivities

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import play.api.libs.json.{Json, Reads, Writes}

case class WhoIsYourAccountantIsUk(isUk: Boolean)

object WhoIsYourAccountantIsUk {

  implicit val formRule: Rule[UrlFormEncoded, WhoIsYourAccountantIsUk] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import utils.MappingUtils.Implicits._
    (__ \ "isUK").read[Boolean].withMessage("error.required.ba.advisor.isuk") flatMap { bool =>
      WhoIsYourAccountantIsUk(bool)
    }
  }

  implicit val formWrites = Write[WhoIsYourAccountantIsUk, UrlFormEncoded] {
    data: WhoIsYourAccountantIsUk => Map(
      "isUK" -> Seq(data.isUk.toString)
    )
  }

  implicit val jsonReads: Reads[WhoIsYourAccountantIsUk] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
      (__ \ "isUK").read[Boolean].map(x => WhoIsYourAccountantIsUk(x)) or
      (__ \ "accountantsAddressPostCode").read[String]
          .map(_ => WhoIsYourAccountantIsUk(true)) or
      (__ \ "accountantsAddressCountry").read[String]
        .map(_ => WhoIsYourAccountantIsUk(false))

  }

  implicit val jsonWrites: Writes[WhoIsYourAccountantIsUk] = Writes {
    isUk: WhoIsYourAccountantIsUk => Json.obj("isUK" -> isUk.isUk )
  }
}





