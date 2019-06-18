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

package models.businessdetails

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}

case class CorrespondenceAddressIsUk(isUk: Option[Boolean])

object CorrespondenceAddressIsUk {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CorrespondenceAddressIsUk] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "isUK").read[Boolean].withMessage("error.required.address.line1")
        .map(x => CorrespondenceAddressIsUk.apply(Option(x)))
    }

  implicit val formWrites = Write[CorrespondenceAddressIsUk, UrlFormEncoded] { a =>
    a.isUk match {
      case Some(true) => Map("isUK" -> Seq("true"))
      case Some(false) => Map("isUK" -> Seq("true"))
      case _ => Map()
    }
  }
}
