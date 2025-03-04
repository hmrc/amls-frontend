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
import models.businessactivities.ExpectedAMLSTurnover
import play.api.libs.json._

sealed trait AMLSTurnover {
  val value: String
}

object AMLSTurnover extends Enumerable.Implicits {

  case object First extends WithName("zeroPlus") with AMLSTurnover {
    override val value: String = "01"
  }
  case object Second extends WithName("fifteenThousandPlus") with AMLSTurnover {
    override val value: String = "02"
  }
  case object Third extends WithName("fiftyThousandPlus") with AMLSTurnover {
    override val value: String = "03"
  }
  case object Fourth extends WithName("oneHundredThousandPlus") with AMLSTurnover {
    override val value: String = "04"
  }
  case object Fifth extends WithName("twoHundredFiftyThousandPlus") with AMLSTurnover {
    override val value: String = "05"
  }
  case object Sixth extends WithName("oneMillionPlus") with AMLSTurnover {
    override val value: String = "06"
  }
  case object Seventh extends WithName("tenMillionPlus") with AMLSTurnover {
    override val value: String = "07"
  }

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[AMLSTurnover] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "turnover").read[String].flatMap[AMLSTurnover] {
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

  implicit val jsonWrites: Writes[AMLSTurnover] = Writes[AMLSTurnover] {
    case First   => Json.obj("turnover" -> "01")
    case Second  => Json.obj("turnover" -> "02")
    case Third   => Json.obj("turnover" -> "03")
    case Fourth  => Json.obj("turnover" -> "04")
    case Fifth   => Json.obj("turnover" -> "05")
    case Sixth   => Json.obj("turnover" -> "06")
    case Seventh => Json.obj("turnover" -> "07")
  }

  implicit def convert(model: AMLSTurnover): ExpectedAMLSTurnover = model match {
    case AMLSTurnover.First   => ExpectedAMLSTurnover.First
    case AMLSTurnover.Second  => ExpectedAMLSTurnover.Second
    case AMLSTurnover.Third   => ExpectedAMLSTurnover.Third
    case AMLSTurnover.Fourth  => ExpectedAMLSTurnover.Fourth
    case AMLSTurnover.Fifth   => ExpectedAMLSTurnover.Fifth
    case AMLSTurnover.Sixth   => ExpectedAMLSTurnover.Sixth
    case AMLSTurnover.Seventh => ExpectedAMLSTurnover.Seventh
    case _                    => throw new Exception("Invalid AMLS turnover")
  }

  val all: Seq[AMLSTurnover] = Seq(
    First,
    Second,
    Third,
    Fourth,
    Fifth,
    Sixth,
    Seventh
  )

  implicit val enumerable: Enumerable[AMLSTurnover] = Enumerable(all.map(v => v.toString -> v): _*)
}
