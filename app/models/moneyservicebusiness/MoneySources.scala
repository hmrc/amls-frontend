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

package models.moneyservicebusiness

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._

case class BankMoneySource(bankNames: String)

case class WholesalerMoneySource(wholesalerNames: String)

case class MoneySources(
  bankMoneySource: Option[BankMoneySource] = None,
  wholesalerMoneySource: Option[WholesalerMoneySource] = None,
  customerMoneySource: Option[Boolean] = None
) {

  def size = List(this.bankMoneySource, this.wholesalerMoneySource, this.customerMoneySource).flatten.size

  def toFormValues: Seq[MoneySource] = {
    import models.moneyservicebusiness.MoneySources._
    Seq(
      if (bankMoneySource.isDefined) Some(Banks) else None,
      if (wholesalerMoneySource.isDefined) Some(Wholesalers) else None,
      customerMoneySource.flatMap(b => if (b) Some(Customers) else None)
    ).flatten
  }

  def toMessages(implicit messages: Messages): Seq[String] = Seq(
    this.bankMoneySource.map(_ => messages("msb.which_currencies.source.banks")),
    this.wholesalerMoneySource.map(_ => messages("msb.which_currencies.source.wholesalers")),
    this.customerMoneySource.flatMap(b => if (b) Some(messages("msb.which_currencies.source.customers")) else None)
  ).flatten
}

sealed trait MoneySource

object MoneySources extends Enumerable.Implicits {

  case object Banks extends WithName("banks") with MoneySource
  case object Wholesalers extends WithName("wholesalers") with MoneySource
  case object Customers extends WithName("customers") with MoneySource

  val all: Seq[MoneySource] = Seq(Banks, Wholesalers, Customers)

  implicit val enumerable: Enumerable[MoneySource] = Enumerable(all.map(v => v.toString -> v): _*)

  val bankMoneySourceWriter = new Writes[Option[BankMoneySource]] {
    override def writes(o: Option[BankMoneySource]): JsValue = o match {
      case Some(x) => Json.obj("bankMoneySource" -> "Yes", "bankNames" -> x.bankNames)
      case _       => Json.obj()
    }
  }

  val wholesalerMoneySourceWriter = new Writes[Option[WholesalerMoneySource]] {
    override def writes(o: Option[WholesalerMoneySource]): JsValue = o match {
      case Some(x) => Json.obj("wholesalerMoneySource" -> "Yes", "wholesalerNames" -> x.wholesalerNames)
      case _       => Json.obj()
    }
  }

  val customerMoneySourceWriter = new Writes[Option[Boolean]] {
    override def writes(o: Option[Boolean]): JsValue = o match {
      case Some(true) => Json.obj("customerMoneySource" -> JsString("Yes"))
      case _          => Json.obj()
    }
  }

  val readBanks = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    ((__ \ "bankMoneySource").readNullable[String] and
      (__ \ "bankNames").readNullable[String])((a, b) =>
      (a, b) match {
        case (Some("Yes"), Some(names)) => Some(BankMoneySource(names))
        case _                          => None
      }
    )
  }

  val readWholesalers = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    ((__ \ "wholesalerMoneySource").readNullable[String] and
      (__ \ "wholesalerNames").readNullable[String])((a, b) =>
      (a, b) match {
        case (Some("Yes"), Some(names)) => Some(WholesalerMoneySource(names))
        case _                          => None
      }
    )
  }

  val readCustomerMoney = {

    import play.api.libs.json._

    (__ \ "customerMoneySource").readNullable[String] map {
      case Some("Yes") => Some(true)
      case _           => None
    }
  }

  implicit val jsonReads: Reads[MoneySources] = {
    import play.api.libs.functional.syntax._
    (readBanks and readWholesalers and readCustomerMoney)((bms, wms, cms) => MoneySources(bms, wms, cms))
  }

  implicit val jsonWrites: Writes[MoneySources] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__.write(bankMoneySourceWriter) and
      __.write(wholesalerMoneySourceWriter) and
      __.write(customerMoneySourceWriter))(x => (x.bankMoneySource, x.wholesalerMoneySource, x.customerMoneySource))
  }
}
