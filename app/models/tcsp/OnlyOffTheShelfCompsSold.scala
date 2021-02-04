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

package models.tcsp

import jto.validation.forms.UrlFormEncoded

sealed trait OnlyOffTheShelfCompsSold
case object OnlyOffTheShelfCompsSoldYes extends OnlyOffTheShelfCompsSold
case object OnlyOffTheShelfCompsSoldNo extends OnlyOffTheShelfCompsSold

object OnlyOffTheShelfCompsSold {
  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, OnlyOffTheShelfCompsSold] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "onlyOffTheShelfCompsSold").read[Boolean].withMessage("error.required.tcsp.off.the.shelf.companies") map {
      case true => OnlyOffTheShelfCompsSoldYes
      case false  => OnlyOffTheShelfCompsSoldNo
    }
  }

  implicit val formWrites: Write[OnlyOffTheShelfCompsSold, UrlFormEncoded] = Write {
    case OnlyOffTheShelfCompsSoldYes => "onlyOffTheShelfCompsSold" -> "true"
    case OnlyOffTheShelfCompsSoldNo => "onlyOffTheShelfCompsSold" -> "false"
  }

  import play.api.libs.json._
  import play.api.libs.json.Reads._

  implicit val jsonReads: Reads[OnlyOffTheShelfCompsSold] =  {
    (__ \ "onlyOffTheShelfCompsSold").read[Boolean] map {
      case true => OnlyOffTheShelfCompsSoldYes
      case false  => OnlyOffTheShelfCompsSoldNo
    }
  }

  implicit val jsonWrite = Writes[OnlyOffTheShelfCompsSold] {
    case OnlyOffTheShelfCompsSoldYes => Json.obj("onlyOffTheShelfCompsSold" -> true)
    case OnlyOffTheShelfCompsSoldNo => Json.obj("onlyOffTheShelfCompsSold" -> false)
  }
}
