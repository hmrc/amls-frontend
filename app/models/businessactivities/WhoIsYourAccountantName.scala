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

package models.businessactivities

import jto.validation.{From, Rule, Write}
import jto.validation.forms.UrlFormEncoded
import models.FormTypes.{basicPunctuationPattern, notEmptyStrip}

case class WhoIsYourAccountantName( accountantsName: String,
                                    accountantsTradingName: Option[String]) {

  def name(newName: String) : WhoIsYourAccountantName = this.copy(accountantsName = newName)
  def tradingName(newTradingName: String) : WhoIsYourAccountantName = this.copy(accountantsTradingName = Some(newTradingName))
}

object WhoIsYourAccountantName {

  import play.api.libs.json._

  val key = "who-is-your-accountant"

  implicit val jsonWrites : Writes[WhoIsYourAccountantName] = Writes[WhoIsYourAccountantName] { data:WhoIsYourAccountantName =>
    Json.obj("accountantsName" -> data.accountantsName,
      "accountantsTradingName" -> data.accountantsTradingName
    )
  }

  implicit val jsonReads : Reads[WhoIsYourAccountantName] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    ((__ \ "accountantsName").read[String] and
      (__ \ "accountantsTradingName").readNullable[String])(WhoIsYourAccountantName.apply _)
  }


  implicit val formWrites = Write[WhoIsYourAccountantName, UrlFormEncoded] {
    data: WhoIsYourAccountantName => Map(
      "name" -> Seq(data.accountantsName),
      "tradingName" -> data.accountantsTradingName.toSeq
    )
  }

  implicit val formRule: Rule[UrlFormEncoded, WhoIsYourAccountantName] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      import utils.MappingUtils.Implicits._

      val nameTypeLength = 140
      val tradingNameTypeLength = 120

      val nameType = notEmptyStrip andThen
        notEmpty.withMessage("error.required.ba.advisor.name") andThen
        maxLength(nameTypeLength).withMessage("error.length.ba.advisor.name") andThen
        basicPunctuationPattern("error.punctuation.ba.advisor.name")

      val tradingNameType = notEmptyStrip andThen
        maxLength(tradingNameTypeLength).withMessage("error.length.ba.advisor.tradingname") andThen
        basicPunctuationPattern("error.punctuation.ba.advisor.tradingname")

      ((__ \ "name").read(nameType) ~
        (__ \ "tradingName").read(optionR(tradingNameType)))(WhoIsYourAccountantName.apply)
    }
}



