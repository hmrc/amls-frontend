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

package models.payments

import play.api.libs.json.{Format, Json}
import play.api.libs.functional.syntax._

case class NextUrl(value: String)

object NextUrl {
  implicit val format: Format[NextUrl] = implicitly[Format[String]].inmap(NextUrl(_), _.value)
}

case class CreatePaymentResponse(nextUrl: NextUrl, paymentId: String)

object CreatePaymentResponse {
  implicit val format = Json.format[CreatePaymentResponse]
}
