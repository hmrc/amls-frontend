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

package models.renewal

import models.{Enumerable, WithName}
import models.businessactivities.ExpectedBusinessTurnover
import play.api.libs.json._

sealed trait BusinessTurnover {
  val value: String
}

object BusinessTurnover extends Enumerable.Implicits {

  case object First extends WithName("zeroPlus") with BusinessTurnover {
    override val value: String = "01"
  }
  case object Second extends WithName("fifteenThousandPlus") with BusinessTurnover {
    override val value: String = "02"
  }
  case object Third extends WithName("fiftyThousandPlus") with BusinessTurnover {
    override val value: String = "03"
  }
  case object Fourth extends WithName("oneHundredThousandPlus") with BusinessTurnover {
    override val value: String = "04"
  }
  case object Fifth extends WithName("twoHundredFiftyThousandPlus") with BusinessTurnover {
    override val value: String = "05"
  }
  case object Sixth extends WithName("oneMillionPlus") with BusinessTurnover {
    override val value: String = "06"
  }
  case object Seventh extends WithName("tenMillionPlus") with BusinessTurnover {
    override val value: String = "07"
  }

  import utils.MappingUtils.Implicits._

  val key = "renewal-business-turnover"

  implicit val jsonReads: Reads[BusinessTurnover] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "businessTurnover").read[String].flatMap[BusinessTurnover] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _    =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonWrites: Writes[BusinessTurnover] = Writes[BusinessTurnover] {
    case First   => Json.obj("businessTurnover" -> "01")
    case Second  => Json.obj("businessTurnover" -> "02")
    case Third   => Json.obj("businessTurnover" -> "03")
    case Fourth  => Json.obj("businessTurnover" -> "04")
    case Fifth   => Json.obj("businessTurnover" -> "05")
    case Sixth   => Json.obj("businessTurnover" -> "06")
    case Seventh => Json.obj("businessTurnover" -> "07")
  }

  implicit def convert(model: BusinessTurnover): ExpectedBusinessTurnover = model match {
    case BusinessTurnover.First   => ExpectedBusinessTurnover.First
    case BusinessTurnover.Second  => ExpectedBusinessTurnover.Second
    case BusinessTurnover.Third   => ExpectedBusinessTurnover.Third
    case BusinessTurnover.Fourth  => ExpectedBusinessTurnover.Fourth
    case BusinessTurnover.Fifth   => ExpectedBusinessTurnover.Fifth
    case BusinessTurnover.Sixth   => ExpectedBusinessTurnover.Sixth
    case BusinessTurnover.Seventh => ExpectedBusinessTurnover.Seventh
    case _                        => throw new Exception("Invalid business turnover value")
  }

  val all: Seq[BusinessTurnover] = Seq(
    First,
    Second,
    Third,
    Fourth,
    Fifth,
    Sixth,
    Seventh
  )

  implicit val enumerable: Enumerable[BusinessTurnover] = Enumerable(all.map(v => v.toString -> v): _*)
}
