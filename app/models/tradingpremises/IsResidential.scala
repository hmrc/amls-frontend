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

package models.tradingpremises

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class IsResidential (isResidential: Boolean)

object IsResidential {

  implicit val format =  Json.format[IsResidential]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, IsResidential] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
    (__ \ "isResidential").read[Boolean].withMessage("tradingpremises.yourtradingpremises.isresidential.required") map IsResidential.apply
  }

  implicit val formWrites = Write[IsResidential, UrlFormEncoded] {
      case IsResidential(value) => Map("isResidential" -> Seq(value.toString))
  }
}
