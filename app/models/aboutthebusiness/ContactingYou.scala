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

package models.aboutthebusiness

import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}
import play.api.libs.json.Json

case class ContactingYou(
                          phoneNumber: String,
                          email: String
                        )

object ContactingYou {

  implicit def convert(c: ContactingYouForm): ContactingYou =
    ContactingYou(
      phoneNumber = c.phoneNumber,
      email = c.email
    )

  implicit val formats = Json.format[ContactingYou]

  implicit val formWrites: Write[ContactingYou, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import scala.Function.unlift
    (
      (__ \ "phoneNumber").write[String] ~
        (__ \ "email").write[String]
      ) (unlift(ContactingYou.unapply _))
  }
}

case class ContactingYouForm(
                              phoneNumber: String,
                              email: String,
                              letterToThisAddress: Boolean
                            )

object ContactingYouForm {

  implicit val formats = Json.format[ContactingYouForm]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ContactingYouForm] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import jto.validation.forms.Rules._
      (
        (__ \ "phoneNumber").read(phoneNumberType) ~
          (__ \ "email").read(emailType) ~
          (__ \ "letterToThisAddress").read[Boolean].withMessage("error.required.rightaddress")
        )(ContactingYouForm.apply _)
    }
}
