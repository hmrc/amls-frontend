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

import jto.validation.{From, Rule, To, Write}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class BusinessPartners(value: String){
  val indexValue = """([0-9]+)$""".r.findFirstIn(value) map {_.toInt}
}

object BusinessPartners {

  import utils.MappingUtils.Implicits._

  val key = "business-partners"

  implicit val formRule: Rule[UrlFormEncoded, BusinessPartners] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "value").read[String].withMessage("error.required.declaration.partners") map BusinessPartners.apply
    }
  implicit val formWrites: Write[BusinessPartners, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (__ \ "value").write[String] contramap{x =>x.value}
  }

  implicit val format = Json.format[BusinessPartners]

}
