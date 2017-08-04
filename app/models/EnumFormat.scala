/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import enumeratum.{Enum, EnumEntry}
import play.api.data.validation.ValidationError
import play.api.libs.json._

object EnumFormat {
  // $COVERAGE-OFF$
  def apply[T <: EnumEntry](e: Enum[T]): Format[T] = Format(
    Reads {
      case JsString(value) => e.withNameOption(value).map(JsSuccess(_))
        .getOrElse(JsError(ValidationError(s"Unknown ${e.getClass.getSimpleName} value: $value", s"error.invalid.${e.getClass.getSimpleName.toLowerCase.replaceAllLiterally("$", "")}")))
      case _ => JsError("Can only parse String")
    },
    Writes(v => JsString(v.entryName))
  )
}
