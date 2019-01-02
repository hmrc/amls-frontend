/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{JsNull, JsResult, Json, Reads}
import uk.gov.hmrc.http.{ HttpReads, HttpResponse }

trait HttpResponseHelper {

  // scalastyle:off object.name
  object & {
    def unapply(response: HttpResponse): Option[(HttpResponse, HttpResponse)] =
      Some((response, response))
  }

  object status {
    def unapply(response: HttpResponse): Option[Int] = Some(response.status)
  }

  class JsonParsed[A] {
    def unapply(response: HttpResponse)(implicit rds: Reads[A]): Option[JsResult[A]] = {
      val json = Option(response.json) getOrElse JsNull
      Some(Json.fromJson[A](json))
    }
  }

  object JsonParsed {
    def apply[A] = new JsonParsed[A]
  }

  implicit val httpReads: HttpReads[HttpResponse] =
    new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse =
        response
    }
}
