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

package models.businessactivities

import models.{Enumerable, WithName}
import models.renewal.AMLSTurnover
import play.api.libs.json._

sealed trait ExpectedAMLSTurnover {
  val value: String
}

object ExpectedAMLSTurnover extends Enumerable.Implicits {

  case object First extends WithName("zeroPlus") with ExpectedAMLSTurnover {
    override val value: String = "01"
  }
  case object Second extends WithName("fifteenThousandPlus") with ExpectedAMLSTurnover {
    override val value: String = "02"
  }
  case object Third extends WithName("fiftyThousandPlus") with ExpectedAMLSTurnover {
    override val value: String = "03"
  }
  case object Fourth extends WithName("oneHundredThousandPlus") with ExpectedAMLSTurnover {
    override val value: String = "04"
  }
  case object Fifth extends WithName("twoHundredFiftyThousandPlus") with ExpectedAMLSTurnover {
    override val value: String = "05"
  }
  case object Sixth extends WithName("oneMillionPlus") with ExpectedAMLSTurnover {
    override val value: String = "06"
  }
  case object Seventh extends WithName("tenMillionPlus") with ExpectedAMLSTurnover {
    override val value: String = "07"
  }

  implicit val jsonReads: Reads[ExpectedAMLSTurnover] =
    (__ \ "expectedAMLSTurnover").read[String].flatMap {
      case "01" => Reads.pure(First)
      case "02" => Reads.pure(Second)
      case "03" => Reads.pure(Third)
      case "04" => Reads.pure(Fourth)
      case "05" => Reads.pure(Fifth)
      case "06" => Reads.pure(Sixth)
      case "07" => Reads.pure(Seventh)
      case _    => Reads(_ => JsError(JsonValidationError("error.invalid")))
    }

  implicit val jsonWrites: Writes[ExpectedAMLSTurnover] = Writes[ExpectedAMLSTurnover] {
    case First   => Json.obj("expectedAMLSTurnover" -> "01")
    case Second  => Json.obj("expectedAMLSTurnover" -> "02")
    case Third   => Json.obj("expectedAMLSTurnover" -> "03")
    case Fourth  => Json.obj("expectedAMLSTurnover" -> "04")
    case Fifth   => Json.obj("expectedAMLSTurnover" -> "05")
    case Sixth   => Json.obj("expectedAMLSTurnover" -> "06")
    case Seventh => Json.obj("expectedAMLSTurnover" -> "07")
  }

  implicit def convert(model: ExpectedAMLSTurnover): AMLSTurnover = model match {
    case ExpectedAMLSTurnover.First   => AMLSTurnover.First
    case ExpectedAMLSTurnover.Second  => AMLSTurnover.Second
    case ExpectedAMLSTurnover.Third   => AMLSTurnover.Third
    case ExpectedAMLSTurnover.Fourth  => AMLSTurnover.Fourth
    case ExpectedAMLSTurnover.Fifth   => AMLSTurnover.Fifth
    case ExpectedAMLSTurnover.Sixth   => AMLSTurnover.Sixth
    case ExpectedAMLSTurnover.Seventh => AMLSTurnover.Seventh
    case _                            => throw new Exception("Invalid AMLS turnover")
  }

  val all: Seq[ExpectedAMLSTurnover] = Seq(
    First,
    Second,
    Third,
    Fourth,
    Fifth,
    Sixth,
    Seventh
  )

  implicit val enumerable: Enumerable[ExpectedAMLSTurnover] =
    Enumerable(ExpectedAMLSTurnover.all.map(v => v.toString -> v): _*)
}
