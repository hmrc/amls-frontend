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

package models.moneyservicebusiness

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import models.{Enumerable, WithName}
import models.renewal.TotalThroughput
import play.api.libs.json._

sealed trait ExpectedThroughput {
  val value: String
}

object ExpectedThroughput extends Enumerable.Implicits {

  def convert(expectedThroughput: ExpectedThroughput): TotalThroughput = {
    expectedThroughput match {
      case First => TotalThroughput("01")
      case Second => TotalThroughput("02")
      case Third => TotalThroughput("03")
      case Fourth => TotalThroughput( "04")
      case Fifth => TotalThroughput("05")
      case Sixth => TotalThroughput("06")
      case Seventh=> TotalThroughput( "07")
    }
  }

  case object First extends WithName("first") with ExpectedThroughput {
    override val value: String = "01"
  }
  case object Second extends WithName("second") with ExpectedThroughput {
    override val value: String = "02"
  }
  case object Third extends WithName("third") with ExpectedThroughput {
    override val value: String = "03"
  }
  case object Fourth extends WithName("fourth") with ExpectedThroughput {
    override val value: String = "04"
  }
  case object Fifth extends WithName("fifth") with ExpectedThroughput {
    override val value: String = "05"
  }
  case object Sixth extends WithName("sixth") with ExpectedThroughput {
    override val value: String = "06"
  }
  case object Seventh extends WithName("seventh") with ExpectedThroughput {
    override val value: String = "07"
  }

  val all: Seq[ExpectedThroughput] = Seq(
    First,
    Second,
    Third,
    Fourth,
    Fifth,
    Sixth,
    Seventh
  )

  implicit val enumerable: Enumerable[ExpectedThroughput] = Enumerable(all.map(v => v.toString -> v): _*)

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExpectedThroughput] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "throughput").read[String].withMessage("error.required.msb.throughput") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "throughput") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[ExpectedThroughput, UrlFormEncoded] = Write {
    case First => "throughput" -> "01"
    case Second => "throughput" -> "02"
    case Third => "throughput" -> "03"
    case Fourth => "throughput" -> "04"
    case Fifth => "throughput" -> "05"
    case Sixth => "throughput" -> "06"
    case Seventh=> "throughput" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "throughput").read[String].flatMap[ExpectedThroughput] {
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

  implicit val jsonWrites = Writes[ExpectedThroughput] {
    case First => Json.obj("throughput" -> "01")
    case Second => Json.obj("throughput" -> "02")
    case Third => Json.obj("throughput" -> "03")
    case Fourth => Json.obj("throughput" -> "04")
    case Fifth => Json.obj("throughput" -> "05")
    case Sixth => Json.obj("throughput" -> "06")
    case Seventh => Json.obj("throughput" -> "07")
  }
}
