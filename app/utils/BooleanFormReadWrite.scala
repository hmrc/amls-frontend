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

package utils

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import utils.MappingUtils.Implicits._
import jto.validation.forms.Rules._

object BooleanFormReadWrite {
  def formWrites(fieldName: String): Write[Boolean, UrlFormEncoded] = Write { data: Boolean => Map(fieldName -> Seq(data.toString)) }

  def formRule(fieldName: String, msg: String): Rule[UrlFormEncoded, Boolean] = From[UrlFormEncoded] { __ =>
    (__ \ fieldName).read[Boolean].withMessage(msg)
  }
}
