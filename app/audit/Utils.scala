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

package audit

import play.api.libs.json._

object Utils {
  def toMap[A](model: A)(implicit writes: Writes[A]) = Json.toJson(model).as[JsObject].value.mapValues {
    case JsString(v) => v // for some reason, if you don't do this, it puts two double quotes around the resulting string
    case v => v.toString
  }

  implicit class JsExtensions(obj: JsObject) {
    def maybeCombine[A](value: (String, Option[A]))(implicit writes: Writes[A]): JsObject = value match {
      case (name, Some(v)) => obj ++ Json.obj(name -> Json.toJsFieldJsValueWrapper[A](v))
      case _ => obj
    }

    def ++?[A](value: (String, Option[A]))(implicit writes: Writes[A]) = maybeCombine(value)
  }
}
