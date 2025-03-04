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
import play.api.libs.json._

sealed trait PercentageOfCashPaymentOver15000 {
  val value: String
}

object PercentageOfCashPaymentOver15000 extends Enumerable.Implicits {

  case object First extends WithName("zeroToTwenty") with PercentageOfCashPaymentOver15000 {
    override val value: String = "01"
  }
  case object Second extends WithName("twentyOneToForty") with PercentageOfCashPaymentOver15000 {
    override val value: String = "02"
  }
  case object Third extends WithName("fortyOneToSixty") with PercentageOfCashPaymentOver15000 {
    override val value: String = "03"
  }
  case object Fourth extends WithName("sixtyOneToEighty") with PercentageOfCashPaymentOver15000 {
    override val value: String = "04"
  }
  case object Fifth extends WithName("eightyOneToOneHundred") with PercentageOfCashPaymentOver15000 {
    override val value: String = "05"
  }

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[PercentageOfCashPaymentOver15000] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "percentage").read[String].flatMap[PercentageOfCashPaymentOver15000] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case _    =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonWrites: Writes[PercentageOfCashPaymentOver15000] = Writes[PercentageOfCashPaymentOver15000] {
    case First  => Json.obj("percentage" -> "01")
    case Second => Json.obj("percentage" -> "02")
    case Third  => Json.obj("percentage" -> "03")
    case Fourth => Json.obj("percentage" -> "04")
    case Fifth  => Json.obj("percentage" -> "05")
  }

  implicit def convert(model: PercentageOfCashPaymentOver15000): models.hvd.PercentageOfCashPaymentOver15000 =
    model match {
      case PercentageOfCashPaymentOver15000.First  => models.hvd.PercentageOfCashPaymentOver15000.First
      case PercentageOfCashPaymentOver15000.Second => models.hvd.PercentageOfCashPaymentOver15000.Second
      case PercentageOfCashPaymentOver15000.Third  => models.hvd.PercentageOfCashPaymentOver15000.Third
      case PercentageOfCashPaymentOver15000.Fourth => models.hvd.PercentageOfCashPaymentOver15000.Fourth
      case PercentageOfCashPaymentOver15000.Fifth  => models.hvd.PercentageOfCashPaymentOver15000.Fifth
      case _                                       => throw new Exception("Invalid PercentageOfCashPaymentOver15000")
    }

  val all: Seq[PercentageOfCashPaymentOver15000] = Seq(
    First,
    Second,
    Third,
    Fourth,
    Fifth
  )

  implicit val enumerable: Enumerable[PercentageOfCashPaymentOver15000] = Enumerable(all.map(v => v.toString -> v): _*)
}
