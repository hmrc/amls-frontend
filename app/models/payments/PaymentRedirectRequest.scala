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

package models.payments

import models.ReturnLocation
import play.api.libs.json.Writes

case class PaymentRedirectRequest(reference: String, amount: Double, redirectUrl: ReturnLocation)

object PaymentRedirectRequest {

  implicit val jsonWriter: Writes[PaymentRedirectRequest] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "reference").write[String] and
        (__ \ "amount").write[String].contramap[Double](_.toString) and
        (__ \ "url").write[String].contramap[ReturnLocation](_.absoluteUrl)
      ) (unlift(PaymentRedirectRequest.unapply))
  }

}
