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

package models.businessmatching

import jto.validation.forms.Rules._
import jto.validation.{From, Rule, Write}
import jto.validation.forms._
import models.FormTypes.{maxRegLength, minWithMsg, notEmptyStrip, regexWithMsg}
import play.api.libs.json.Json

case class CompanyRegistrationNumber(companyRegistrationNumber: String)


object CompanyRegistrationNumber {

  import utils.MappingUtils.Implicits._

  val registrationNumberRegex = "^[A-Z0-9]{8}$".r
  val registrationType = notEmpty.withMessage("error.required.bm.registration.number") andThen
    pattern(registrationNumberRegex).withMessage("error.invalid.bm.registration.number")

  def CompanyRegNumberRule(requiredMsg: String = "error.required.bm.registration.number",
                           minLengthMsg: String = "error.invalid.bm.registration.number.length",
                           regExMessage: String = "error.invalid.bm.registration.number.allowed") =
    notEmptyStrip
      .andThen(notEmpty.withMessage(requiredMsg))
      .andThen(minWithMsg(maxRegLength, minLengthMsg))
      .andThen(regexWithMsg(registrationNumberRegex, regExMessage))

  implicit val formats = Json.format[CompanyRegistrationNumber]

  implicit val formReads: Rule[UrlFormEncoded, CompanyRegistrationNumber] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "companyRegistrationNumber").read(CompanyRegNumberRule()) map CompanyRegistrationNumber.apply
  }

  implicit val formWrites: Write[CompanyRegistrationNumber, UrlFormEncoded] = Write {
    case CompanyRegistrationNumber(registered) => Map("companyRegistrationNumber" -> Seq(registered))
  }
}
