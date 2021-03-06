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

package models.payments

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}

case class TypeOfBank(isUK: Boolean)

object TypeOfBank {

  val pathName = "typeOfBank"

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, TypeOfBank] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ pathName).read[Boolean].withMessage("payments.typeofbank.error") map TypeOfBank.apply
    }

  implicit val formWrites = Write[TypeOfBank, UrlFormEncoded] {
    case TypeOfBank(value) => Map(pathName -> Seq(value.toString))
  }

}
