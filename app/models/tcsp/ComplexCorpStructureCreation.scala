/*
 * Copyright 2023 HM Revenue & Customs
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

sealed trait ComplexCorpStructureCreation
case object ComplexCorpStructureCreationYes extends ComplexCorpStructureCreation
case object ComplexCorpStructureCreationNo extends ComplexCorpStructureCreation

object ComplexCorpStructureCreation {
  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, ComplexCorpStructureCreation] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "complexCorpStructureCreation").read[Boolean].withMessage("error.required.tcsp.complex.corporate.structures") map {
      case true => ComplexCorpStructureCreationYes
      case false  => ComplexCorpStructureCreationNo
    }
  }

  implicit val formWrites: Write[ComplexCorpStructureCreation, UrlFormEncoded] = Write {
    case ComplexCorpStructureCreationYes => "complexCorpStructureCreation" -> "true"
    case ComplexCorpStructureCreationNo => "complexCorpStructureCreation" -> "false"
  }

  import play.api.libs.json._
  import play.api.libs.json.Reads._

  implicit val jsonReads: Reads[ComplexCorpStructureCreation] =  {
    (__ \ "complexCorpStructureCreation").read[Boolean] map {
      case true => ComplexCorpStructureCreationYes
      case false  => ComplexCorpStructureCreationNo
    }
  }

  implicit val jsonWrite = Writes[ComplexCorpStructureCreation] {
    case ComplexCorpStructureCreationYes => Json.obj("complexCorpStructureCreation" -> true)
    case ComplexCorpStructureCreationNo => Json.obj("complexCorpStructureCreation" -> false)
  }
}

