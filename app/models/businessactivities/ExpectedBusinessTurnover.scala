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

package models.businessactivities

import models.renewal.BusinessTurnover
import models.{Enumerable, WithName}
import play.api.libs.json._

sealed trait ExpectedBusinessTurnover {
  val value: String
}

object ExpectedBusinessTurnover extends Enumerable.Implicits {

  case object First extends WithName("zeroPlus") with ExpectedBusinessTurnover {
    override val value: String = "01"
  }
  case object Second extends WithName("fifteenThousandPlus") with ExpectedBusinessTurnover {
    override val value: String = "02"
  }
  case object Third extends WithName("fiftyThousandPlus") with ExpectedBusinessTurnover {
    override val value: String = "03"
  }
  case object Fourth extends WithName("oneHundredThousandPlus") with ExpectedBusinessTurnover {
    override val value: String = "04"
  }
  case object Fifth extends WithName("twoHundredFiftyThousandPlus") with ExpectedBusinessTurnover {
    override val value: String = "05"
  }
  case object Sixth extends WithName("oneMillionPlus") with ExpectedBusinessTurnover {
    override val value: String = "06"
  }
  case object Seventh extends WithName("tenMillionPlus") with ExpectedBusinessTurnover {
    override val value: String = "07"
  }

  import utils.MappingUtils.Implicits._

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "expectedBusinessTurnover").read[String].flatMap[ExpectedBusinessTurnover] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[ExpectedBusinessTurnover] {
    case First => Json.obj("expectedBusinessTurnover" -> "01")
    case Second => Json.obj("expectedBusinessTurnover" -> "02")
    case Third => Json.obj("expectedBusinessTurnover" -> "03")
    case Fourth => Json.obj("expectedBusinessTurnover" -> "04")
    case Fifth => Json.obj("expectedBusinessTurnover" -> "05")
    case Sixth => Json.obj("expectedBusinessTurnover" -> "06")
    case Seventh => Json.obj("expectedBusinessTurnover" -> "07")
  }

  def convert(model: ExpectedBusinessTurnover): BusinessTurnover = model match {
    case ExpectedBusinessTurnover.First => BusinessTurnover.First
    case ExpectedBusinessTurnover.Second => BusinessTurnover.Second
    case ExpectedBusinessTurnover.Third => BusinessTurnover.Third
    case ExpectedBusinessTurnover.Fourth => BusinessTurnover.Fourth
    case ExpectedBusinessTurnover.Fifth => BusinessTurnover.Fifth
    case ExpectedBusinessTurnover.Sixth => BusinessTurnover.Sixth
    case ExpectedBusinessTurnover.Seventh => BusinessTurnover.Seventh
    case _ => throw new Exception("Invalid business turnover value")
  }

  val all: Seq[ExpectedBusinessTurnover] = Seq(
    First,
    Second,
    Third,
    Fourth,
    Fifth,
    Sixth,
    Seventh
  )

  implicit val enumerable: Enumerable[ExpectedBusinessTurnover] = Enumerable(all.map(v => v.toString -> v): _*)
}
