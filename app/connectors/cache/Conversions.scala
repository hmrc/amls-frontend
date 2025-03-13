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

package connectors.cache

import play.api.libs.json.{JsObject, JsValue}
// $COVERAGE-OFF$
trait Conversions {

  /** Maps a JSValue to a Map of String -> JsValue
    * @param json
    *   The json to convert
    * @return
    *   A map, where the keys of the input json form the keys of the output Map
    */
  def toMap(json: JsValue): Map[String, JsValue] = json match {
    case JsObject(fields) => fields.toMap
    case _                => Map.empty[String, JsValue]
  }
}
// $COVERAGE-ON$
