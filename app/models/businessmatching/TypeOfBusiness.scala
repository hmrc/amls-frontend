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

package models.businessmatching

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import play.api.libs.json._
import models.FormTypes._

case class TypeOfBusiness(typeOfBusiness: String)

object TypeOfBusiness{


  implicit val format = Json.format[TypeOfBusiness]

  implicit val formRead:Rule[UrlFormEncoded, TypeOfBusiness] = From[UrlFormEncoded] {__ =>
    import jto.validation.forms.Rules._

    val maxTypeOfBusinessLength = 40
    val typeOfBusinessLength = maxWithMsg(maxTypeOfBusinessLength, "error.max.length.bm.businesstype.type")
    val typeOfBusinessRequired = required("error.required.bm.businesstype.type")
    val typeOfBusinessType = notEmptyStrip andThen typeOfBusinessRequired andThen typeOfBusinessLength andThen basicPunctuationPattern()

    (__ \ "typeOfBusiness").read(typeOfBusinessType) map TypeOfBusiness.apply
  }

  implicit val formWrite: Write[TypeOfBusiness, UrlFormEncoded] = Write {
    case TypeOfBusiness(p) => Map("typeOfBusiness" -> Seq(p.toString))
  }

}
