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

package utils

import play.api.mvc.AnyContent

object CharacterCountParser {

  def cleanData(requestBody: AnyContent, fieldName: String): Map[String, Seq[String]] = {
    val incomingData: Map[String, Seq[String]] = requestBody.asFormUrlEncoded.getOrElse(Map.empty)
    incomingData.map { case (key, values) =>
      if (key == fieldName) {
        (key, values.map(_.replace("\r\n", "\n")))
      } else {
        (key, values)
      }
    }
  }
}
