/*
 * Copyright 2018 HM Revenue & Customs
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

import enumeratum._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import models.EnumFormatForm

sealed abstract class WaysToPay extends EnumEntry

object WaysToPay extends PlayEnum[WaysToPay] {

  import utils.MappingUtils.Implicits._

  case object Card extends WaysToPay
  case object Bacs extends WaysToPay

  override def values = findValues

  val pathName = "waysToPay"

  implicit val formRule: Rule[UrlFormEncoded, WaysToPay] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ pathName).read(EnumFormatForm.reader(WaysToPay)).withMessage("payments.waystopay.error")
  }

  implicit val formWrites: Write[WaysToPay, UrlFormEncoded] = Write { __ =>
    Map(pathName -> Seq(__.entryName))
  }

}
